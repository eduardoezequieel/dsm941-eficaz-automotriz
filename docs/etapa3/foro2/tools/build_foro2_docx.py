#!/usr/bin/env python3
"""Build INVESTIGACION-FIREBASE.docx and .pdf from INVESTIGACION-FIREBASE.md.

The DOCX is the source; the PDF is LibreOffice's export of that same DOCX, so both
match page by page. Format follows the Foro 1 document: APA 7, Times New Roman 12,
double spacing, UDB logo on the cover, "Contenido" with dot leaders, unnumbered
headings, each level-1 heading on a new page.

    pip install python-docx pillow      # LibreOffice (soffice) and pdftotext on PATH
    python3 tools/build_foro2_docx.py
"""

from __future__ import annotations

import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
MD = ROOT / "INVESTIGACION-FIREBASE.md"
DOCX_PATH = ROOT / "INVESTIGACION-FIREBASE.docx"
PDF_PATH = ROOT / "INVESTIGACION-FIREBASE.pdf"
LOGO = ROOT / "img" / "udb-logo.png"
TEXT_WIDTH_IN = 6.5
FONT = "Times New Roman"
MONO = "Courier New"


# ---------- markdown ----------

def parse(text: str):
    lines = text.splitlines()
    title, cover, i = "", [], 0
    while i < len(lines) and not lines[i].startswith("## "):
        s = lines[i].strip()
        if s.startswith("# "):
            title = s[2:]
        elif s and not s.startswith("<!--"):
            cover.append(s)
        i += 1
    blocks, buf = [], []

    def flush():
        if buf:
            blocks.append(("p", " ".join(buf)))
            buf.clear()

    while i < len(lines):
        line, s = lines[i], lines[i].strip()
        if s.startswith("```"):
            flush(); i += 1; code = []
            while i < len(lines) and not lines[i].strip().startswith("```"):
                code.append(lines[i]); i += 1
            blocks.append(("code", code)); i += 1; continue
        if line.startswith("## "):
            flush(); blocks.append(("h1", line[3:].strip())); i += 1; continue
        if line.startswith("### "):
            flush(); blocks.append(("h2", line[4:].strip())); i += 1; continue
        if line.startswith("|"):
            flush(); rows = []
            while i < len(lines) and lines[i].startswith("|"):
                if not re.match(r"^\|\s*-+", lines[i]):
                    rows.append([c.strip() for c in lines[i].strip().strip("|").split("|")])
                i += 1
            blocks.append(("table", rows)); continue
        if s.startswith("!["):
            flush()
            m = re.match(r"!\[(.*?)\]\((.*?)\)(?:\{([a-z]+)\})?$", s)
            blocks.append(("img", (m.group(2), m.group(3) or ""))); i += 1; continue
        if re.match(r"^\d+\.\s", s):
            flush(); items = []
            while i < len(lines) and re.match(r"^\d+\.\s", lines[i].strip()):
                items.append(re.sub(r"^\d+\.\s", "", lines[i].strip())); i += 1
            blocks.append(("ol", items)); continue
        if not s:
            flush(); i += 1; continue
        buf.append(s); i += 1
    flush()
    return title, cover, blocks


# ---------- low-level helpers ----------

def font(run, size=12, bold=None, italic=None, mono=False):
    name = MONO if mono else FONT
    run.font.name = name
    run.font.size = Pt(size)
    run.font.color.rgb = RGBColor(0, 0, 0)
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    for attr in ("w:ascii", "w:hAnsi", "w:eastAsia", "w:cs"):
        rfonts.set(qn(attr), name)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic


def fmt(p, *, indent=True, align="justify", spacing=2.0, before=0, after=0,
        hanging=False, left=0.0, keep_next=False):
    pf = p.paragraph_format
    pf.space_before, pf.space_after = Pt(before), Pt(after)
    pf.line_spacing = spacing
    pf.left_indent = Inches(left + (0.5 if hanging else 0))
    pf.first_line_indent = Inches(-0.5 if hanging else (0.5 if indent else 0))
    pf.keep_with_next = keep_next
    pf.widow_control = True
    p.alignment = {"justify": WD_ALIGN_PARAGRAPH.JUSTIFY, "left": WD_ALIGN_PARAGRAPH.LEFT,
                   "center": WD_ALIGN_PARAGRAPH.CENTER, "right": WD_ALIGN_PARAGRAPH.RIGHT}[align]


INLINE = re.compile(r"(\*\*.+?\*\*|`[^`]+`|\*[^*]+\*)")


def inline(p, text, size=12, mono_size=10):
    pos = 0
    for m in INLINE.finditer(text):
        if m.start() > pos:
            font(p.add_run(text[pos:m.start()]), size)
        tok = m.group(0)
        if tok.startswith("**"):
            font(p.add_run(tok[2:-2]), size, bold=True)
        elif tok.startswith("`"):
            # zero-width spaces let long identifiers wrap in narrow cells
            font(p.add_run(tok[1:-1].replace("_", "_​").replace(".", ".​")), mono_size, mono=True)
        else:
            font(p.add_run(tok[1:-1]), size, italic=True)
        pos = m.end()
    if pos < len(text):
        font(p.add_run(text[pos:]), size)


def field(run, instr):
    for kind, text in (("begin", None), ("instr", instr), ("separate", None), ("end", None)):
        if kind == "instr":
            el = OxmlElement("w:instrText"); el.set(qn("xml:space"), "preserve"); el.text = instr
        else:
            el = OxmlElement("w:fldChar"); el.set(qn("w:fldCharType"), kind)
        run._r.append(el)


def shade(p, fill="F2F2F2"):
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear"); shd.set(qn("w:color"), "auto"); shd.set(qn("w:fill"), fill)
    pPr = p._p.get_or_add_pPr()
    for tag in ("w:tabs", "w:suppressAutoHyphens", "w:spacing", "w:ind", "w:jc"):
        el = pPr.find(qn(tag))
        if el is not None:
            el.addprevious(shd); return
    pPr.append(shd)


def cell_borders(cell, top=None, bottom=None):
    tcPr = cell._tc.get_or_add_tcPr()
    b = OxmlElement("w:tcBorders")
    for edge, val in (("top", top), ("left", None), ("bottom", bottom), ("right", None)):
        e = OxmlElement(f"w:{edge}")
        if val:
            e.set(qn("w:val"), "single"); e.set(qn("w:sz"), str(val)); e.set(qn("w:color"), "000000")
        else:
            e.set(qn("w:val"), "nil")
        e.set(qn("w:space"), "0")
        b.append(e)
    tcPr.append(b)


def cell_width(cell, twips):
    tcPr = cell._tc.get_or_add_tcPr()
    w = tcPr.find(qn("w:tcW"))
    if w is None:
        w = OxmlElement("w:tcW"); tcPr.insert(0, w)
    w.set(qn("w:w"), str(twips)); w.set(qn("w:type"), "dxa")


def no_split_row(row):
    trPr = row._tr.get_or_add_trPr()
    trPr.append(OxmlElement("w:cantSplit"))


def header_row(row):
    trPr = row._tr.get_or_add_trPr()
    trPr.append(OxmlElement("w:tblHeader"))


def bookmark(p, name, bid):
    start = OxmlElement("w:bookmarkStart"); start.set(qn("w:id"), str(bid)); start.set(qn("w:name"), name)
    end = OxmlElement("w:bookmarkEnd"); end.set(qn("w:id"), str(bid))
    p._p.insert(1 if p._p.pPr is not None else 0, start)
    p._p.append(end)


# ---------- document parts ----------

def setup(doc):
    s = doc.sections[0]
    s.page_width, s.page_height = Inches(8.5), Inches(11)
    for side in ("left", "right", "top", "bottom"):
        setattr(s, f"{side}_margin", Inches(1))
    s.header_distance = s.footer_distance = Inches(0.5)
    hp = s.header.paragraphs[0]
    fmt(hp, indent=False, align="right", spacing=1.0)
    r = hp.add_run(); font(r); field(r, " PAGE ")

    st = doc.styles
    normal = st["Normal"]
    normal.font.name, normal.font.size = FONT, Pt(12)
    normal.element.get_or_add_rPr().get_or_add_rFonts().set(qn("w:eastAsia"), FONT)
    for name, center in (("Heading 1", True), ("Heading 2", False)):
        h = st[name]
        h.font.name, h.font.size, h.font.bold, h.font.italic = FONT, Pt(12), True, False
        h.font.color.rgb = RGBColor(0, 0, 0)
        rf = h.element.get_or_add_rPr().get_or_add_rFonts()
        for attr in ("w:ascii", "w:hAnsi", "w:eastAsia", "w:cs"):
            rf.set(qn(attr), FONT)
        for attr in ("w:asciiTheme", "w:hAnsiTheme", "w:eastAsiaTheme", "w:cstheme"):
            if rf.get(qn(attr)) is not None:
                del rf.attrib[qn(attr)]
        pf = h.paragraph_format
        pf.space_before = Pt(0); pf.space_after = Pt(0); pf.line_spacing = 2.0
        pf.keep_with_next = True
        pf.first_line_indent = Inches(0)
        pf.alignment = WD_ALIGN_PARAGRAPH.CENTER if center else WD_ALIGN_PARAGRAPH.LEFT
        pf.page_break_before = center
    for lvl, left in ((1, 0.0), (2, 0.3)):
        t = st[f"TOC {lvl}"] if f"TOC {lvl}" in [x.name for x in st] else st.add_style(f"TOC {lvl}", 1)
        t.base_style = normal
        t.font.name, t.font.size, t.font.bold = FONT, Pt(12), (lvl == 1)
        pf = t.paragraph_format
        pf.left_indent = Inches(left); pf.space_after = Pt(0); pf.line_spacing = 1.5
        pf.tab_stops.add_tab_stop(Inches(TEXT_WIDTH_IN), WD_TAB_ALIGNMENT.RIGHT, WD_TAB_LEADER.DOTS)

    zoom = doc.settings.element.find(qn("w:zoom"))
    if zoom is not None:
        zoom.set(qn("w:percent"), "100")
    cp = doc.core_properties
    cp.title = "Firebase aplicado a Eficaz Automotriz"
    cp.author = "Equipo Eficaz Automotriz - DSM941"
    cp.subject = "DSM941 · Etapa 3 · Foro 2: Integración Firebase"
    cp.language = "es-SV"


def trimmed_logo() -> Path:
    src = Image.open(LOGO).convert("RGBA")
    img = Image.new("RGB", src.size, "white")
    img.paste(src, mask=src.split()[3])
    gray = img.convert("L").point(lambda v: 255 if v < 245 else 0)
    box = gray.getbbox()
    out = Path(tempfile.gettempdir()) / "udb-logo-trim.png"
    img.crop(box).save(out)
    return out


def cover(doc, title, lines):
    p = doc.add_paragraph(); fmt(p, indent=False, align="center", spacing=1.0, before=24, after=36)
    p.add_run().add_picture(str(trimmed_logo()), width=Inches(2.9))
    p = doc.add_paragraph(); fmt(p, indent=False, align="center", after=24)
    font(p.add_run(title), bold=True)
    groups = [[], [], [], []]   # authors, institution, stage, date
    for ln in lines:
        if re.search(r"\([A-Z]{2}\d{6}\)$", ln):
            groups[0].append(ln)
        elif ln.startswith(("Universidad", "DSM941", "Docente")):
            groups[1].append(ln)
        elif ln.startswith(("Etapa", "Ciclo")):
            groups[2].append(ln)
        else:
            groups[3].append(ln)
    for g in groups:
        for k, ln in enumerate(g):
            p = doc.add_paragraph(); fmt(p, indent=False, align="center", after=(24 if k == len(g) - 1 else 0))
            font(p.add_run(ln))


def toc(doc, headings, pages):
    p = doc.add_paragraph(); fmt(p, indent=False, align="center", after=12)
    p.paragraph_format.page_break_before = True
    font(p.add_run("Contenido"), bold=True)
    first = True
    for k, (lvl, text, bm) in enumerate(headings):
        p = doc.add_paragraph(style=f"TOC {lvl}")
        if first:
            r = p.add_run()
            for kind, val in (("begin", None), ("instr", ' TOC \\o "1-2" \\h \\z \\u '), ("separate", None)):
                if kind == "instr":
                    el = OxmlElement("w:instrText"); el.set(qn("xml:space"), "preserve"); el.text = val
                else:
                    el = OxmlElement("w:fldChar"); el.set(qn("w:fldCharType"), kind)
                r._r.append(el)
            first = False
        link = OxmlElement("w:hyperlink"); link.set(qn("w:anchor"), bm); link.set(qn("w:history"), "1")
        p._p.append(link)
        for t in (text, "\t", str(pages.get(bm, ""))):
            r = p.add_run(); font(r, bold=(lvl == 1))
            if t == "\t":
                r._r.append(OxmlElement("w:tab"))
            else:
                r.text = t
            link.append(r._r)
        if k == len(headings) - 1:
            r = p.add_run(); el = OxmlElement("w:fldChar"); el.set(qn("w:fldCharType"), "end"); r._r.append(el)


def caption(doc, label, title):
    p = doc.add_paragraph(); fmt(p, indent=False, align="left", before=12, keep_next=True)
    font(p.add_run(label), bold=True)
    p = doc.add_paragraph(); fmt(p, indent=False, align="left", keep_next=True)
    inline(p, title.strip("*"))
    for r in p.runs:
        r.italic = True


def note(doc, text):
    p = doc.add_paragraph(); fmt(p, indent=False, align="left", spacing=1.0, after=12)
    inline(p, text, size=10, mono_size=9)


def table(doc, rows):
    ncol = len(rows[0])
    total = int(TEXT_WIDTH_IN * 1440)
    lens = [max(len(re.sub(r"[`*]", "", r[c])) for r in rows) for c in range(ncol)]
    weights = [max(12, min(l, 60)) for l in lens]
    widths = [int(total * w / sum(weights)) for w in weights]
    widths[-1] = total - sum(widths[:-1])
    t = doc.add_table(rows=len(rows), cols=ncol)
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    grid = t._tbl.tblGrid
    for gc, w in zip(grid.findall(qn("w:gridCol")), widths):
        gc.set(qn("w:w"), str(w))
    tblPr = t._tbl.tblPr
    tw = tblPr.find(qn("w:tblW"))
    if tw is None:
        tw = OxmlElement("w:tblW"); tblPr.insert(1, tw)
    tw.set(qn("w:w"), str(total)); tw.set(qn("w:type"), "dxa")
    for ri, row in enumerate(rows):
        no_split_row(t.rows[ri])
        if ri == 0:
            header_row(t.rows[ri])
        for ci, text in enumerate(row):
            cell = t.rows[ri].cells[ci]
            cell_width(cell, widths[ci])
            cell_borders(cell, top=(8 if ri == 0 else None), bottom=(4 if ri == 0 else (8 if ri == len(rows) - 1 else None)))
            p = cell.paragraphs[0]
            fmt(p, indent=False, align="left", spacing=1.0, before=2, after=2, keep_next=(ri < len(rows) - 1))
            inline(p, text, size=10, mono_size=9)
            if ri == 0:
                for r in p.runs:
                    r.bold = True


def picture(doc, path: Path, phone: bool):
    w, h = Image.open(path).size
    max_w = 2.6 if phone else TEXT_WIDTH_IN
    max_h = 6.3
    width = min(max_w, w / 96)
    if width * h / w > max_h:
        width = max_h * w / h
    p = doc.add_paragraph(); fmt(p, indent=False, align="center", spacing=1.0, before=4, after=4, keep_next=True)
    p.add_run().add_picture(str(path), width=Inches(width))


def body(doc, blocks):
    bid, headings, refs = 100, [], False
    i = 0
    while i < len(blocks):
        kind, data = blocks[i]
        if kind in ("h1", "h2"):
            lvl = 1 if kind == "h1" else 2
            p = doc.add_paragraph(style=f"Heading {lvl}")
            if lvl == 2:
                p.paragraph_format.space_before = Pt(6)
            r = p.add_run(data); font(r, bold=True)
            bm = f"_Toc{bid}"; bookmark(p, bm, bid); bid += 1
            headings.append((lvl, data, bm))
            refs = (lvl == 1 and data == "Referencias")
            i += 1; continue
        cap = re.fullmatch(r"\*\*(Figura|Tabla) (\d+)\*\*", data) if kind == "p" else None
        if cap and i + 2 < len(blocks):
            caption(doc, f"{cap.group(1)} {cap.group(2)}", blocks[i + 1][1])
            target = blocks[i + 2]
            if target[0] == "img":
                picture(doc, ROOT / target[1][0], target[1][1] == "phone")
            else:
                table(doc, target[1])
            i += 3
            if i < len(blocks) and blocks[i][0] == "p" and blocks[i][1].startswith("*Nota.*"):
                note(doc, blocks[i][1]); i += 1
            elif target[0] == "table":
                doc.add_paragraph().paragraph_format.space_after = Pt(0)
            continue
        if kind == "table":
            table(doc, data); i += 1
            if i < len(blocks) and blocks[i][0] == "p" and blocks[i][1].startswith("*Nota.*"):
                note(doc, blocks[i][1]); i += 1
            continue
        if kind == "code":
            for k, ln in enumerate(data):
                p = doc.add_paragraph(); fmt(p, indent=False, align="left", spacing=1.0,
                                             before=(6 if k == 0 else 0), after=(6 if k == len(data) - 1 else 0),
                                             keep_next=(k < len(data) - 1))
                shade(p)
                font(p.add_run(ln if ln else " "), size=8.5, mono=True)
            if i + 1 < len(blocks) and blocks[i + 1][0] == "p" and blocks[i + 1][1].startswith("*Nota.*"):
                note(doc, blocks[i + 1][1]); i += 1
            i += 1; continue
        if kind == "ol":
            for n, item in enumerate(data, 1):
                p = doc.add_paragraph(); fmt(p, indent=False, align="left", hanging=True, left=0.0)
                inline(p, f"{n}.\t{item}")
            i += 1; continue
        if kind == "p":
            p = doc.add_paragraph()
            if refs:
                fmt(p, indent=False, align="left", hanging=True)
            elif data.startswith("*Nota.*"):
                note(doc, data); p._p.getparent().remove(p._p); i += 1; continue
            else:
                fmt(p, indent=True, align="justify")
            inline(p, data)
            i += 1; continue
        i += 1
    return headings


def build(pages):
    title, cover_lines, blocks = parse(MD.read_text(encoding="utf-8"))
    doc = Document()
    setup(doc)
    cover(doc, title, cover_lines)
    headings = [(1 if k == "h1" else 2, d, f"_Toc{100 + n}") for n, (k, d) in
                enumerate([b for b in blocks if b[0] in ("h1", "h2")])]
    toc(doc, headings, pages)
    body(doc, blocks)
    doc.save(DOCX_PATH)
    return headings


# ---------- PDF + page numbers ----------

SOFFICE_HELPER = Path("/sessions") .glob("*/mnt/.claude/skills/docx/scripts/office/soffice.py")


def to_pdf():
    out = Path(tempfile.mkdtemp())
    helper = next(iter(SOFFICE_HELPER), None)
    cmd = ([sys.executable, str(helper)] if helper else [shutil.which("soffice") or "soffice"])
    subprocess.run(cmd + ["--headless", "--convert-to", "pdf", "--outdir", str(out), str(DOCX_PATH)],
                   check=True, capture_output=True)
    shutil.copy(out / (DOCX_PATH.stem + ".pdf"), PDF_PATH)


def find_pages(headings):
    text = subprocess.run(["pdftotext", "-layout", str(PDF_PATH), "-"], capture_output=True, text=True).stdout
    pages = text.split("\f")
    found, start = {}, 2   # skip cover and Contenido
    norm = lambda s: re.sub(r"\s+", " ", s).strip()
    for lvl, h, bm in headings:
        for n in range(start, len(pages)):
            lines = [norm(l) for l in pages[n].splitlines()]
            if norm(h) in lines:
                found[bm] = n + 1; start = n; break
    missing = [h for _, h, bm in headings if bm not in found]
    if missing:
        raise SystemExit(f"headings not found in PDF: {missing}")
    return found


def check_citations():
    text = MD.read_text(encoding="utf-8")
    bodytxt, _, refs = text.partition("## Referencias")
    cited = set(re.findall(r"\((?:[^()]*?; )?((?:Google|Cloudinary|Román Gallardo et al\.), (?:s\. f\.|\d{4}[a-z]?))\)", bodytxt))
    keys = set()
    for ln in refs.splitlines():
        m = re.match(r"^(Google|Cloudinary)\. \((s\. f\.|\d{4}[a-z]?)", ln)
        if m:
            keys.add(f"{m.group(1)}, {m.group(2)}")
        elif ln.startswith("Román Gallardo"):
            keys.add("Román Gallardo et al., " + re.search(r"\((\d{4})\)", ln).group(1))
    if cited != keys:
        raise SystemExit(f"citation mismatch: not in refs={sorted(cited - keys)} unused={sorted(keys - cited)}")
    print(f"citations ok: {len(keys)} references")


def main():
    check_citations()
    pages = {}
    for attempt in range(3):
        headings = build(pages)
        to_pdf()
        new = find_pages(headings)
        if new == pages:
            break
        pages = new
    print("pages:", {h: pages[bm] for _, h, bm in headings})
    print(DOCX_PATH, PDF_PATH)


if __name__ == "__main__":
    main()
