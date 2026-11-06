#!/usr/bin/env python3
"""Build the Foro 2 PDF and DOCX from INVESTIGACION-FIREBASE.md.

Libraries stay outside the repository:

    pip3 install --target ~/.cache/foro2-docgen/py pymupdf python-docx
    npm install --prefix ~/.cache/foro2-docgen puppeteer-core

Chrome (already installed) prints the PDF. The script renders the SVG
diagrams to PNG, writes HTML, prints it, reads the page of each heading,
and writes the DOCX with that same índice.
"""

from __future__ import annotations

import html
import os
import re
import subprocess
import sys
import textwrap
from pathlib import Path

CACHE = Path.home() / ".cache" / "foro2-docgen"
sys.path.insert(0, str(CACHE / "py"))

import fitz  # noqa: E402
from docx import Document  # noqa: E402
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING  # noqa: E402
from docx.oxml import OxmlElement  # noqa: E402
from docx.oxml.ns import qn  # noqa: E402
from docx.shared import Cm, Emu, Inches, Pt, RGBColor  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
MD = ROOT / "INVESTIGACION-FIREBASE.md"
IMG = ROOT / "img"
HTML_PATH = ROOT / "tools" / ".build.html"
PDF_PATH = ROOT / "INVESTIGACION-FIREBASE.pdf"
DOCX_PATH = ROOT / "INVESTIGACION-FIREBASE.docx"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
PUPPETEER = CACHE / "node_modules" / "puppeteer-core"

CITATION_RE = re.compile(
    r"\((Google, 2026[a-z]|Cloudinary, 2026|Román Gallardo et al\., 2020)\)"
)


def render_svgs() -> None:
    for svg in sorted(IMG.glob("*.svg")):
        png = svg.with_suffix(".png")
        doc = fitz.open(svg)
        page = doc[0]
        scale = 2
        pix = page.get_pixmap(matrix=fitz.Matrix(scale, scale), alpha=False)
        pix.save(png)
        doc.close()
        print(f"png {png.name} {pix.width}x{pix.height}")


def parse(text: str):
    lines = text.splitlines()
    title = ""
    cover = []
    i = 0
    while i < len(lines) and not lines[i].startswith("## "):
        line = lines[i]
        if line.startswith("# "):
            title = line[2:].strip()
        elif line.strip() and not line.strip().startswith("<!--"):
            cover.append(line.strip())
        i += 1
    blocks = []
    buf: list[str] = []

    def flush():
        if buf:
            blocks.append(("p", " ".join(buf).strip()))
            buf.clear()

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        if stripped.startswith("<!--") and stripped.endswith("-->"):
            flush()
            i += 1
            continue
        if stripped.startswith("```"):
            flush()
            i += 1
            code = []
            while i < len(lines) and not lines[i].strip().startswith("```"):
                code.append(lines[i])
                i += 1
            blocks.append(("code", "\n".join(code).rstrip("\n")))
            i += 1
            continue
        if line.startswith("## "):
            flush()
            blocks.append(("h1", line[3:].strip()))
            i += 1
            continue
        if line.startswith("### "):
            flush()
            blocks.append(("h2", line[4:].strip()))
            i += 1
            continue
        if line.startswith("|"):
            flush()
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                if not re.match(r"^\|\s*-+", lines[i]):
                    cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
                    rows.append(cells)
                i += 1
            blocks.append(("table", rows))
            continue
        if stripped.startswith("!["):
            flush()
            match = re.match(r"!\[(.*?)\]\((.*?)\)(?:\{([a-z]+)\})?$", stripped)
            blocks.append(("img", (match.group(1), match.group(2), match.group(3) or "")))
            i += 1
            continue
        if re.match(r"^\d+\.\s", stripped):
            flush()
            items = []
            while i < len(lines) and re.match(r"^\d+\.\s", lines[i].strip()):
                items.append(re.sub(r"^\d+\.\s", "", lines[i].strip()))
                i += 1
            blocks.append(("ol", items))
            continue
        if stripped == "":
            flush()
            i += 1
            continue
        buf.append(stripped)
        i += 1
    flush()
    return title, cover, blocks


def cell_text(text: str) -> str:
    """Let a long Firebase code wrap after an underscore instead of mid-word."""
    return text.replace("_", "_\u200b")


def inline_html(text: str) -> str:
    out = []
    i = 0
    while i < len(text):
        if text.startswith("**", i):
            end = text.find("**", i + 2)
            if end != -1:
                out.append("<strong>" + inline_html(text[i + 2 : end]) + "</strong>")
                i = end + 2
                continue
        if text.startswith("`", i):
            end = text.find("`", i + 1)
            if end != -1:
                out.append("<code>" + html.escape(text[i + 1 : end]) + "</code>")
                i = end + 1
                continue
        if text.startswith("*", i):
            end = text.find("*", i + 1)
            if end != -1:
                out.append("<em>" + inline_html(text[i + 1 : end]) + "</em>")
                i = end + 1
                continue
        out.append(html.escape(text[i]))
        i += 1
    return "".join(out)


def is_caption(kind: str, data, word: str) -> bool:
    return kind == "p" and re.fullmatch(rf"\*\*{word} \d+\*\*", data or "") is not None


CSS = """
@page {
  size: letter;
  margin: 1in;
  @top-right {
    content: counter(page);
    font-family: "Times New Roman", Times, serif;
    font-size: 12pt;
    color: #000;
  }
}
html, body {
  margin: 0; padding: 0;
  font-family: "Times New Roman", Times, serif;
  font-size: 12pt;
  line-height: 2;
  color: #000;
}
p { margin: 0; text-indent: 0.5in; }
h1.sec, h2.sec {
  font-size: 12pt; line-height: 2; margin: 0; font-weight: 700;
  page-break-after: avoid; font-family: "Times New Roman", Times, serif;
}
h1.sec { text-align: center; }
h1.sec.break-before { break-before: page; page-break-before: always; }
h2.sec { text-align: left; }
.cover {
  min-height: 8.4in;
  display: flex; align-items: center; justify-content: center;
  text-align: center; page-break-after: always;
}
.cover-inner p { text-indent: 0; margin: 0; }
.cover-inner .title { font-weight: 700; }
.toc { page-break-after: always; }
.toc h1 { text-align: center; font-size: 12pt; font-weight: 700; margin: 0 0 12pt; }
.toc-row { display: flex; align-items: flex-end; line-height: 1.7; }
.toc-row.sub { padding-left: 0.35in; }
.toc-row .label { flex: 0 1 auto; }
.toc-row .dots {
  flex: 1 1 auto; border-bottom: 1px dotted #000;
  margin: 0 6px 0.35em; min-width: 1em;
}
.toc-row .num { flex: 0 0 auto; }
.cap { text-align: left; text-indent: 0; margin: 12pt 0 0; page-break-after: avoid; }
.cap .num { font-weight: 700; }
.cap .title { font-style: italic; }
table {
  width: 100%; border-collapse: collapse; line-height: 1.2;
  font-size: 10.5pt; margin: 4pt 0 0;
}
th, td {
  border: 1px solid #000; padding: 3px 5px; vertical-align: top; text-align: left;
  overflow-wrap: anywhere;
}
th { font-weight: 700; background: #f3f3f3; }
tr { break-inside: avoid; }
.note { text-indent: 0; margin: 0 0 10pt; }
.figure { margin: 8pt 0 10pt; page-break-inside: avoid; }
.figure img { width: 100%; height: auto; display: block; }
.figure.phone img { width: 7.5cm; margin-left: auto; margin-right: auto; }
pre {
  font-family: "Courier New", Courier, monospace;
  font-size: 8.5pt; line-height: 1.28; white-space: pre-wrap;
  background: #f4f4f4; padding: 6px 8px; margin: 6pt 0 0;
  border: 1px solid #ccc;
}
ol { margin: 0 0 0 0.25in; padding-left: 0.35in; }
li { margin: 0; padding: 0; }
.captura {
  text-indent: 0; margin: 0;
  border-left: 2.5pt solid #000; padding-left: 0.15in;
}
.refs p {
  text-indent: -0.5in; margin-left: 0.5in; margin-top: 0; margin-bottom: 0;
  overflow-wrap: anywhere;
}
code { font-family: "Courier New", Courier, monospace; font-size: 10.5pt; }
"""


def heading_id(text: str) -> str:
    slug = re.sub(r"[^0-9a-zA-ZáéíóúñÁÉÍÓÚÑ]+", "-", text).strip("-").lower()
    return slug


def build_html(title: str, cover: list[str], blocks, toc: dict[str, int] | None) -> str:
    parts = [
        "<!DOCTYPE html><html lang='es'><head><meta charset='utf-8'>",
        f"<title>{html.escape(title)}</title><style>{CSS}</style></head><body>",
        "<section class='cover'><div class='cover-inner'>",
        f"<p class='title'>{html.escape(title)}</p>",
    ]
    for line in cover:
        cls = "title" if line.startswith("DSM941") or line.startswith("Universidad") else ""
        parts.append(f"<p class='{cls}'>{html.escape(line)}</p>")
    parts.append("</div></section>")
    parts.append("<section class='toc'><h1>Índice</h1>")
    for kind, data in blocks:
        if kind not in ("h1", "h2"):
            continue
        page = "" if toc is None else str(toc.get(data, ""))
        cls = "toc-row sub" if kind == "h2" else "toc-row"
        parts.append(
            f"<div class='{cls}'><span class='label'>{html.escape(data)}</span>"
            f"<span class='dots'></span><span class='num'>{page}</span></div>"
        )
    parts.append("</section>")

    i = 0
    in_refs = False
    while i < len(blocks):
        kind, data = blocks[i]
        if kind == "h1" and data.startswith("9."):
            in_refs = True
        if kind == "h1":
            cls = "sec break-before" if data.startswith("9.") else "sec"
            parts.append(f"<h1 class='{cls}' id='{heading_id(data)}'>{html.escape(data)}</h1>")
            i += 1
            continue
        if kind == "h2":
            parts.append(f"<h2 class='sec' id='{heading_id(data)}'>{html.escape(data)}</h2>")
            i += 1
            continue
        if is_caption(kind, data, "Figura") and i + 2 < len(blocks) and blocks[i + 2][0] == "img":
            number = re.search(r"\d+", data).group(0)
            title_html = inline_html(blocks[i + 1][1])
            src = blocks[i + 2][1][1]
            phone = len(blocks[i + 2][1]) > 2 and blocks[i + 2][1][2] == "phone"
            parts.append("<div class='figure phone'>" if phone else "<div class='figure'>")
            parts.append(f"<p class='cap'><span class='num'>Figura {number}</span><br>{title_html}</p>")
            parts.append(f"<img src='../{html.escape(src)}' alt='Figura {number}'>")
            i += 3
            if i < len(blocks) and blocks[i][0] == "p" and blocks[i][1].startswith("*Nota.*"):
                parts.append(f"<p class='note'>{inline_html(blocks[i][1])}</p>")
                i += 1
            parts.append("</div>")
            continue
        if is_caption(kind, data, "Tabla") and i + 2 < len(blocks) and blocks[i + 2][0] == "table":
            number = re.search(r"\d+", data).group(0)
            title_html = inline_html(blocks[i + 1][1])
            rows = blocks[i + 2][1]
            parts.append("<div class='table-block'>")
            parts.append(f"<p class='cap'><span class='num'>Tabla {number}</span><br>{title_html}</p>")
            parts.append("<table><thead><tr>")
            for cell in rows[0]:
                parts.append(f"<th>{inline_html(cell_text(cell))}</th>")
            parts.append("</tr></thead><tbody>")
            for row in rows[1:]:
                parts.append("<tr>")
                for cell in row:
                    parts.append(f"<td>{inline_html(cell_text(cell))}</td>")
                parts.append("</tr>")
            parts.append("</tbody></table>")
            i += 3
            if i < len(blocks) and blocks[i][0] == "p" and blocks[i][1].startswith("*Nota.*"):
                parts.append(f"<p class='note'>{inline_html(blocks[i][1])}</p>")
                i += 1
            parts.append("</div>")
            continue
        if kind == "p":
            cls = "refs" if in_refs else ""
            extra = "captura" if data.startswith("[CAPTURA:") else ""
            wrap = f"<div class='{cls}'>" if cls else ""
            end = "</div>" if cls else ""
            parts.append(f"{wrap}<p class='{extra}'>{inline_html(data)}</p>{end}")
            i += 1
            continue
        if kind == "code":
            parts.append(f"<pre>{html.escape(data)}</pre>")
            i += 1
            continue
        if kind == "ol":
            parts.append("<ol>")
            for item in data:
                parts.append(f"<li>{inline_html(item)}</li>")
            parts.append("</ol>")
            i += 1
            continue
        if kind == "table":
            parts.append("<table><thead><tr>")
            for cell in data[0]:
                parts.append(f"<th>{inline_html(cell_text(cell))}</th>")
            parts.append("</tr></thead><tbody>")
            for row in data[1:]:
                parts.append("<tr>")
                for cell in row:
                    parts.append(f"<td>{inline_html(cell_text(cell))}</td>")
                parts.append("</tr>")
            parts.append("</tbody></table>")
            i += 1
            if i < len(blocks) and blocks[i][0] == "p" and blocks[i][1].startswith("*Nota.*"):
                parts.append(f"<p class='note'>{inline_html(blocks[i][1])}</p>")
                i += 1
            continue
        if kind == "img":
            parts.append(f"<img src='../{html.escape(data[1])}' alt=''>")
            i += 1
            continue
        i += 1
    parts.append("</body></html>")
    return "\n".join(parts)


def print_pdf(html_path: Path, pdf_path: Path) -> None:
    script = CACHE / "print-pdf.mjs"
    script.write_text(
        textwrap.dedent(
            f"""
            import puppeteer from {str(PUPPETEER / "lib" / "esm" / "puppeteer" / "puppeteer-core.js")!r};
            const browser = await puppeteer.launch({{
              executablePath: {CHROME!r},
              headless: true,
              args: ["--no-sandbox", "--font-render-hinting=none"],
            }});
            const page = await browser.newPage();
            await page.goto({html_path.as_uri()!r}, {{ waitUntil: "networkidle0" }});
            await page.pdf({{
              path: {str(pdf_path)!r},
              format: "Letter",
              printBackground: true,
              preferCSSPageSize: true,
              displayHeaderFooter: false,
              margin: {{ top: "0", right: "0", bottom: "0", left: "0" }},
            }});
            await browser.close();
            """
        ).strip()
        + "\n",
        encoding="utf-8",
    )
    # puppeteer-core's package export path varies. Resolve it from node.
    node = textwrap.dedent(
        f"""
        import puppeteer from 'puppeteer-core';
        const browser = await puppeteer.launch({{
          executablePath: {CHROME!r},
          headless: true,
          args: ['--no-sandbox', '--font-render-hinting=none'],
        }});
        const page = await browser.newPage();
        await page.goto({html_path.as_uri()!r}, {{ waitUntil: 'networkidle0' }});
        await page.pdf({{
          path: {str(pdf_path)!r},
          format: 'Letter',
          printBackground: true,
          preferCSSPageSize: true,
          displayHeaderFooter: false,
          margin: {{ top: '0', right: '0', bottom: '0', left: '0' }},
        }});
        await browser.close();
        """
    )
    env = os.environ.copy()
    env["NODE_PATH"] = str(CACHE / "node_modules")
    subprocess.run(["node", "--input-type=module", "-e", node], check=True, cwd=str(CACHE), env=env)


def heading_pages(pdf_path: Path, headings: list[str]) -> dict[str, int]:
    doc = fitz.open(pdf_path)
    texts = []
    for page in doc:
        texts.append(re.sub(r"\s+", " ", page.get_text("text")))
    doc.close()
    body_start = 1
    for index, text in enumerate(texts, start=1):
        if "1. Introducción" in text and "Índice" not in text:
            body_start = index
            break
    found = {}
    for heading in headings:
        for index, text in enumerate(texts, start=1):
            if index < body_start:
                continue
            if heading in text:
                found[heading] = index
                break
        else:
            found[heading] = 0
    return found


def set_run_font(run, name="Times New Roman", size=12, bold=False, italic=False, font="times"):
    run.bold = bold
    run.italic = italic
    run.font.name = name
    run.font.size = Pt(size)
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    if font == "mono":
        run.font.name = "Courier New"
        run._element.rPr.rFonts.set(qn("w:eastAsia"), "Courier New")


def shade_paragraph(paragraph, color="F4F4F4"):
    pPr = paragraph._p.get_or_add_pPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), color)
    pPr.append(shd)


def add_page_field(paragraph):
    run = paragraph.add_run()
    set_run_font(run, size=12)
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = " PAGE "
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.append(begin)
    run._r.append(instr)
    run._r.append(end)


def body_format(paragraph, *, indent=True, center=False, hanging=False, after=0, before=0, spacing=2):
    pf = paragraph.paragraph_format
    pf.space_before = Pt(before)
    pf.space_after = Pt(after)
    pf.line_spacing = spacing
    if hanging:
        pf.left_indent = Inches(0.5)
        pf.first_line_indent = Inches(-0.5)
    elif indent:
        pf.first_line_indent = Inches(0.5)
    else:
        pf.first_line_indent = Inches(0)
    if center:
        paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER


def add_inline(paragraph, text: str, size=12, mono_size=10.5):
    pattern = re.compile(r"(\*\*.*?\*\*|`.*?`|\*.*?\*)")
    pos = 0
    for match in pattern.finditer(text):
        if match.start() > pos:
            run = paragraph.add_run(text[pos:match.start()])
            set_run_font(run, size=size)
        token = match.group(0)
        if token.startswith("**"):
            run = paragraph.add_run(token[2:-2])
            set_run_font(run, size=size, bold=True)
        elif token.startswith("`"):
            run = paragraph.add_run(token[1:-1])
            set_run_font(run, size=mono_size, font="mono")
        else:
            run = paragraph.add_run(token[1:-1])
            set_run_font(run, size=size, italic=True)
        pos = match.end()
    if pos < len(text):
        run = paragraph.add_run(text[pos:])
        set_run_font(run, size=size)


def set_cell_border(cell):
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    tcBorders = OxmlElement("w:tcBorders")
    for edge in ("top", "left", "bottom", "right"):
        element = OxmlElement(f"w:{edge}")
        element.set(qn("w:val"), "single")
        element.set(qn("w:sz"), "4")
        element.set(qn("w:space"), "0")
        element.set(qn("w:color"), "000000")
        tcBorders.append(element)
    tcPr.append(tcBorders)


def shade_cell(cell):
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), "F3F3F3")
    tcPr.append(shd)


def add_table(document, rows):
    table = document.add_table(rows=len(rows), cols=len(rows[0]))
    table.autofit = True
    for r, row in enumerate(rows):
        for c, text in enumerate(row):
            cell = table.rows[r].cells[c]
            cell.text = ""
            p = cell.paragraphs[0]
            body_format(p, indent=False, spacing=1.0)
            add_inline(p, cell_text(text), size=10.5)
            if r == 0:
                for run in p.runs:
                    run.bold = True
                shade_cell(cell)
            set_cell_border(cell)
    document.add_paragraph()


def add_picture_fitted(document, path: Path, max_width_in=6.4, center=False):
    with fitz.open(path) as image:
        # PNG via fitz gives page size in points if opened as pdf; use PIL-less pixel size.
        pass
    pix_doc = fitz.open(path)
    rect = pix_doc[0].rect
    pix_doc.close()
    # fitz image width is in pixels stored as the page size in points at 72 dpi
    # when opening a PNG. Width in inches = rect.width / 72.
    width_in = rect.width / 72
    height_in = rect.height / 72
    if width_in <= 0:
        width_in, height_in = 6.4, 4
    scale = min(1.0, max_width_in / width_in)
    document.add_picture(str(path), width=Inches(width_in * scale))
    if center:
        document.paragraphs[-1].alignment = WD_ALIGN_PARAGRAPH.CENTER


def build_docx(title, cover, blocks, toc):
    document = Document()
    section = document.sections[0]
    section.page_width = Inches(8.5)
    section.page_height = Inches(11)
    section.left_margin = Inches(1)
    section.right_margin = Inches(1)
    section.top_margin = Inches(1)
    section.bottom_margin = Inches(1)
    section.header_distance = Inches(0.5)
    section.footer_distance = Inches(0.5)
    header = section.header.paragraphs[0]
    header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    body_format(header, indent=False, spacing=1)
    add_page_field(header)

    normal = document.styles["Normal"]
    normal.font.name = "Times New Roman"
    normal.font.size = Pt(12)
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Times New Roman")

    def cover_line(text, bold=False):
        p = document.add_paragraph()
        body_format(p, indent=False, center=True)
        run = p.add_run(text)
        set_run_font(run, bold=bold)

    for _ in range(3):
        cover_line("")
    cover_line(title, bold=True)
    cover_line("")
    for line in cover:
        cover_line(line, bold=line.startswith("DSM941") or line.startswith("Universidad") or line.startswith("8 de"))
    document.add_page_break()

    p = document.add_paragraph()
    body_format(p, indent=False, center=True)
    run = p.add_run("Índice")
    set_run_font(run, bold=True)
    for kind, data in blocks:
        if kind not in ("h1", "h2"):
            continue
        p = document.add_paragraph()
        body_format(p, indent=False, spacing=2)
        if kind == "h2":
            p.paragraph_format.left_indent = Inches(0.35)
        run = p.add_run(data)
        set_run_font(run)
        run2 = p.add_run("  " + "." * 12 + "  " + str(toc.get(data, "")))
        set_run_font(run2)

    document.add_page_break()

    in_refs = False
    i = 0
    while i < len(blocks):
        kind, data = blocks[i]
        if kind == "h1" and data.startswith("9."):
            in_refs = True
            document.add_page_break()
        if kind in ("h1", "h2"):
            p = document.add_paragraph()
            body_format(p, indent=False, center=(kind == "h1"), before=12)
            run = p.add_run(data)
            set_run_font(run, bold=True)
            i += 1
            continue
        if is_caption(kind, data, "Figura") and i + 2 < len(blocks) and blocks[i + 2][0] == "img":
            number = re.search(r"\d+", data).group(0)
            p = document.add_paragraph()
            body_format(p, indent=False, before=12)
            run = p.add_run(f"Figura {number}")
            set_run_font(run, bold=True)
            p2 = document.add_paragraph()
            body_format(p2, indent=False)
            add_inline(p2, blocks[i + 1][1])
            image = ROOT / blocks[i + 2][1][1]
            phone = len(blocks[i + 2][1]) > 2 and blocks[i + 2][1][2] == "phone"
            # 7.5 cm keeps a single phone screenshot readable inside the text block.
            add_picture_fitted(
                document,
                image,
                max_width_in=(7.5 / 2.54) if phone else 6.4,
                center=phone,
            )
            i += 3
            if i < len(blocks) and blocks[i][0] == "p" and blocks[i][1].startswith("*Nota.*"):
                note = document.add_paragraph()
                body_format(note, indent=False)
                add_inline(note, blocks[i][1])
                i += 1
            continue
        if is_caption(kind, data, "Tabla") and i + 2 < len(blocks) and blocks[i + 2][0] == "table":
            number = re.search(r"\d+", data).group(0)
            p = document.add_paragraph()
            body_format(p, indent=False, before=12)
            run = p.add_run(f"Tabla {number}")
            set_run_font(run, bold=True)
            p2 = document.add_paragraph()
            body_format(p2, indent=False)
            add_inline(p2, blocks[i + 1][1])
            add_table(document, blocks[i + 2][1])
            i += 3
            if i < len(blocks) and blocks[i][0] == "p" and blocks[i][1].startswith("*Nota.*"):
                note = document.add_paragraph()
                body_format(note, indent=False)
                add_inline(note, blocks[i][1])
                i += 1
            continue
        if kind == "code":
            for line in data.split("\n"):
                p = document.add_paragraph()
                body_format(p, indent=False, spacing=1.0)
                shade_paragraph(p)
                run = p.add_run(line if line else " ")
                set_run_font(run, size=8, font="mono")
            i += 1
            continue
        if kind == "ol":
            for item in data:
                p = document.add_paragraph()
                body_format(p, indent=False)
                p.paragraph_format.left_indent = Inches(0.5)
                add_inline(p, item)
            i += 1
            continue
        if kind == "table":
            add_table(document, data)
            i += 1
            if i < len(blocks) and blocks[i][0] == "p" and blocks[i][1].startswith("*Nota.*"):
                note = document.add_paragraph()
                body_format(note, indent=False)
                add_inline(note, blocks[i][1])
                i += 1
            continue
        if kind == "p":
            p = document.add_paragraph()
            if in_refs:
                body_format(p, hanging=True)
            elif data.startswith("[CAPTURA:"):
                body_format(p, indent=False)
                p.paragraph_format.left_indent = Inches(0.25)
            else:
                body_format(p, indent=True)
            add_inline(p, data)
            i += 1
            continue
        i += 1
    document.save(DOCX_PATH)


def check_citations(md_text: str) -> None:
    body, _, refs = md_text.partition("## 9. Referencias")
    cited = CITATION_RE.findall(body)
    ref_lines = [line.strip() for line in refs.splitlines() if line.strip() and not line.startswith("#")]
    keys = []
    for line in ref_lines:
        google = re.match(r"^Google\. \((2026[a-z]),", line)
        if google:
            keys.append(f"Google, {google.group(1)}")
        elif line.startswith("Cloudinary. (2026,"):
            keys.append("Cloudinary, 2026")
        elif line.startswith("Román"):
            keys.append("Román Gallardo et al., 2020")
    print("CITATIONS", len(cited), "UNIQUE", sorted(set(cited)))
    print("REFERENCES", keys)
    missing_refs = sorted(set(cited) - set(keys))
    unused = sorted(set(keys) - set(cited))
    if missing_refs or unused or len(keys) != len(set(keys)):
        raise SystemExit(f"citation mismatch missing={missing_refs} unused={unused}")
    print("citation check ok", len(keys), "references")


def main():
    render_svgs()
    text = MD.read_text(encoding="utf-8")
    check_citations(text)
    title, cover, blocks = parse(text)
    headings = [data for kind, data in blocks if kind in ("h1", "h2")]
    HTML_PATH.write_text(build_html(title, cover, blocks, None), encoding="utf-8")
    print_pdf(HTML_PATH, PDF_PATH)
    toc = heading_pages(PDF_PATH, headings)
    print("TOC", toc)
    if any(page <= 0 for page in toc.values()):
        missing = [k for k, v in toc.items() if v <= 0]
        raise SystemExit(f"headings not found in PDF: {missing}")
    HTML_PATH.write_text(build_html(title, cover, blocks, toc), encoding="utf-8")
    print_pdf(HTML_PATH, PDF_PATH)
    # Confirm the second pass still lands headings on the printed numbers.
    confirmed = heading_pages(PDF_PATH, headings)
    drifted = {k: (toc[k], confirmed[k]) for k in toc if toc[k] != confirmed[k]}
    if drifted:
        print("TOC drift, rewriting once", drifted)
        HTML_PATH.write_text(build_html(title, cover, blocks, confirmed), encoding="utf-8")
        print_pdf(HTML_PATH, PDF_PATH)
        toc = confirmed
    build_docx(title, cover, blocks, toc)
    HTML_PATH.unlink(missing_ok=True)
    doc = fitz.open(PDF_PATH)
    print(f"PDF pages {doc.page_count} bytes {PDF_PATH.stat().st_size}")
    page1 = doc[0].get_text("text")[:400]
    print("--- page 1 ---")
    print(page1)
    doc.close()
    print(f"DOCX bytes {DOCX_PATH.stat().st_size}")


if __name__ == "__main__":
    main()
