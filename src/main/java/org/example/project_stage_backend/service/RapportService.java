package org.example.project_stage_backend.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.example.project_stage_backend.dto.IndicateursDTO;
import org.example.project_stage_backend.dto.PerteDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RapportService {

    private final IndicateurService indicateurService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Merged view of both tables for one timestamp
    record LigneRapport(
            LocalDateTime date,
            // Pertes
            Double se, Double syn, Double intVal,
            // Indicateurs calculés
            Double rc, Double ri, Double cap,
            Double consoH2so4, Double consoEauBrute,
            Double consoPhosphates, Double consoVapeur
    ) {}

    public byte[] genererRapport24h() throws Exception {
        LocalDateTime fin   = LocalDateTime.now();
        LocalDateTime debut = fin.minusHours(24);

        List<IndicateursDTO> indicateurs =
                indicateurService.getIndicateursSurPeriode(debut, fin);

        // Fetch pertes over same period via existing repo method exposed in service
        List<PerteDTO> pertes =
                indicateurService.getPertesSurPeriode(debut, fin);

        // Join by closest date (within 1 min) — simple approach: map pertes by truncated minute
        Map<LocalDateTime, PerteDTO> perteByMinute = pertes.stream()
                .collect(Collectors.toMap(
                        p -> p.getDate().withSecond(0).withNano(0),
                        p -> p,
                        (a, b) -> a));

        List<LigneRapport> lignes = indicateurs.stream().map(ind -> {
            LocalDateTime key = ind.getDate().withSecond(0).withNano(0);
            PerteDTO p = perteByMinute.get(key);
            return new LigneRapport(
                    ind.getDate(),
                    p != null ? p.getSe()     : null,
                    p != null ? p.getSyn()    : null,
                    p != null ? p.getIntVal() : null,
                    ind.getRc(), ind.getRi(), ind.getCap(),
                    ind.getConsoH2so4(), ind.getConsoEauBrute(),
                    ind.getConsoPhosphates(), ind.getConsoVapeur()
            );
        }).collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle titleStyle    = createTitleStyle(wb);
            CellStyle subTitleStyle = createSubTitleStyle(wb);
            CellStyle normalStyle   = createNormalStyle(wb, false);
            CellStyle alertStyle    = createNormalStyle(wb, true);
            CellStyle altStyle      = createAltRowStyle(wb);
            CellStyle sectionStyle  = createSectionStyle(wb);
            CellStyle infoValStyle  = createInfoValueStyle(wb);

            // ── Sheet 1 : Résumé ────────────────────────────────
            XSSFSheet resume = wb.createSheet("Résumé");
            resume.setColumnWidth(0, 9000);
            for (int c = 1; c <= 4; c++) resume.setColumnWidth(c, 4000);

            int row = 0;

            Row rTitre = resume.createRow(row++);
            rTitre.setHeightInPoints(30);
            Cell cTitre = rTitre.createCell(0);
            cTitre.setCellValue("RAPPORT JOURNALIER — JFC3");
            cTitre.setCellStyle(titleStyle);
            resume.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            Row rSub = resume.createRow(row++);
            Cell cSub = rSub.createCell(0);
            cSub.setCellValue("Acide Phosphorique — Unité JFC1");
            cSub.setCellStyle(subTitleStyle);
            resume.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

            row++;

            Row rInfo = resume.createRow(row++);
            rInfo.setHeightInPoints(36);
            createInfoCell(wb, rInfo, 0, "PÉRIODE DÉBUT", debut.format(FMT), infoValStyle);
            createInfoCell(wb, rInfo, 1, "PÉRIODE FIN",   fin.format(FMT),   infoValStyle);
            createInfoCell(wb, rInfo, 2, "RELEVÉS",        String.valueOf(lignes.size()), infoValStyle);

            row++;

            // ── Stats Pertes ────────────────────────────────────
            addSectionTitle(resume, row++, "◉  RÉSUMÉ — PERTES GYPSE", sectionStyle);

            Row rHdr1 = resume.createRow(row++);
            for (int i = 0; i < new String[]{"INDICATEUR","MIN","MAX","MOYENNE","SEUIL"}.length; i++) {
                Cell c = rHdr1.createCell(i);
                c.setCellValue(new String[]{"INDICATEUR","MIN","MAX","MOYENNE","SEUIL"}[i]);
                c.setCellStyle(headerStyle);
            }

            if (!lignes.isEmpty()) {
                addStatRow(resume, row++, "SE — Perte Séchage",
                        calcStat(lignes, LigneRapport::se), "≤ 1.5", normalStyle, alertStyle, s -> s.avg() > 1.5);
                addStatRow(resume, row++, "SYN — Perte Synthèse",
                        calcStat(lignes, LigneRapport::syn), "≤ 1.8", normalStyle, alertStyle, s -> s.avg() > 1.8);
                addStatRow(resume, row++, "INT — Perte Intermédiaire",
                        calcStat(lignes, LigneRapport::intVal), "≤ 1.2", normalStyle, alertStyle, s -> s.avg() > 1.2);
            }

            row++;

            // ── Stats Indicateurs ───────────────────────────────
            addSectionTitle(resume, row++, "◉  RÉSUMÉ — INDICATEURS CALCULÉS", sectionStyle);

            Row rHdr2 = resume.createRow(row++);
            for (int i = 0; i < new String[]{"INDICATEUR","MIN","MAX","MOYENNE","SEUIL"}.length; i++) {
                Cell c = rHdr2.createCell(i);
                c.setCellValue(new String[]{"INDICATEUR","MIN","MAX","MOYENNE","SEUIL"}[i]);
                c.setCellStyle(headerStyle);
            }

            if (!lignes.isEmpty()) {
                addStatRow(resume, row++, "RC — Rendement Conc.",
                        calcStat(lignes, LigneRapport::rc), "≥ 0.90", normalStyle, alertStyle, s -> s.avg() < 0.90);
                addStatRow(resume, row++, "RI — Rendement Incorp.",
                        calcStat(lignes, LigneRapport::ri), "≥ 0.85", normalStyle, alertStyle, s -> s.avg() < 0.85);
                addStatRow(resume, row++, "CAP — Capacité Prod.",
                        calcStat(lignes, LigneRapport::cap), "≥ 1.0",  normalStyle, alertStyle, s -> s.avg() < 1.0);
            }

            row++;

            // ── Alertes ─────────────────────────────────────────
            addSectionTitle(resume, row++, "⚠  ANALYSE DES DÉPASSEMENTS", sectionStyle);

            Row rAHdr = resume.createRow(row++);
            for (int i = 0; i < new String[]{"INDICATEUR","NB DÉPASSEMENTS","TOTAL","TAUX (%)","CRITIQUE"}.length; i++) {
                Cell c = rAHdr.createCell(i);
                c.setCellValue(new String[]{"INDICATEUR","NB DÉPASSEMENTS","TOTAL","TAUX (%)","CRITIQUE"}[i]);
                c.setCellStyle(headerStyle);
            }

            int total = lignes.size();
            addAlerteRow(resume, row++, "SE  (≤ 1.5)",  count(lignes, l -> l.se()     != null && l.se()     > 1.5),  total, normalStyle, alertStyle);
            addAlerteRow(resume, row++, "SYN (≤ 1.8)",  count(lignes, l -> l.syn()    != null && l.syn()    > 1.8),  total, normalStyle, alertStyle);
            addAlerteRow(resume, row++, "INT (≤ 1.2)",  count(lignes, l -> l.intVal() != null && l.intVal() > 1.2),  total, normalStyle, alertStyle);
            addAlerteRow(resume, row++, "RC  (≥ 0.90)", count(lignes, l -> l.rc()     != null && l.rc()     < 0.90), total, normalStyle, alertStyle);
            addAlerteRow(resume, row++, "RI  (≥ 0.85)", count(lignes, l -> l.ri()     != null && l.ri()     < 0.85), total, normalStyle, alertStyle);

            // ── Sheet 2 : Historique ────────────────────────────
            XSSFSheet hist = wb.createSheet("Historique Détaillé");
            hist.setColumnWidth(0, 6500);
            int[] colWidths = {3200, 3200, 3200, 3200, 3200, 3200, 3500, 3500, 3500, 3500};
            for (int i = 0; i < colWidths.length; i++) hist.setColumnWidth(i + 1, colWidths[i]);

            hist.createFreezePane(0, 1);

            Row hHdr = hist.createRow(0);
            String[] cols = {"DATE/HEURE","SE","SYN","INT","RC","RI","CAP","H₂SO₄","EAU BRUTE","PHOSPHATES","VAPEUR"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = hHdr.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            boolean pair = false;
            int hRow = 1;
            for (LigneRapport l : lignes) {
                Row r = hist.createRow(hRow++);
                CellStyle base = pair ? altStyle : normalStyle;
                pair = !pair;

                setCellStr(r, 0,  l.date() != null ? l.date().format(FMT) : "—", base);
                setCellNum(r, 1,  l.se(),        base, alertStyle, v -> v > 1.5);
                setCellNum(r, 2,  l.syn(),       base, alertStyle, v -> v > 1.8);
                setCellNum(r, 3,  l.intVal(),    base, alertStyle, v -> v > 1.2);
                setCellNum(r, 4,  l.rc(),        base, alertStyle, v -> v < 0.90);
                setCellNum(r, 5,  l.ri(),        base, alertStyle, v -> v < 0.85);
                setCellNum(r, 6,  l.cap(),       base, alertStyle, v -> v < 1.0);
                setCellNum(r, 7,  l.consoH2so4(),      base, alertStyle, v -> v > 3.2);
                setCellNum(r, 8,  l.consoEauBrute(),   base, alertStyle, v -> false);
                setCellNum(r, 9,  l.consoPhosphates(), base, alertStyle, v -> false);
                setCellNum(r, 10, l.consoVapeur(),     base, alertStyle, v -> false);
            }

            hist.setAutoFilter(new CellRangeAddress(0, 0, 0, 10));

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void addSectionTitle(Sheet sheet, int rowIdx, String title, CellStyle style) {
        Row r = sheet.createRow(rowIdx);
        Cell c = r.createCell(0);
        c.setCellValue(title);
        c.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));
    }

    private void addStatRow(Sheet sheet, int rowIdx, String nom, StatBlock s,
                            String seuil, CellStyle normal, CellStyle alert,
                            java.util.function.Predicate<StatBlock> alertPred) {
        boolean isAlert = alertPred.test(s);
        CellStyle style = isAlert ? alert : normal;
        Row r = sheet.createRow(rowIdx);
        setCellStr(r, 0, nom, style);
        setCellDbl(r, 1, s.min(), style);
        setCellDbl(r, 2, s.max(), style);
        setCellDbl(r, 3, s.avg(), style);
        setCellStr(r, 4, seuil + (isAlert ? " ⚠" : " ✓"), style);
    }

    private void addAlerteRow(Sheet sheet, int rowIdx, String nom, long nb, int total,
                              CellStyle normal, CellStyle alert) {
        double taux = total > 0 ? (nb * 100.0 / total) : 0;
        boolean critique = taux > 20;
        CellStyle style = critique ? alert : normal;
        Row r = sheet.createRow(rowIdx);
        setCellStr(r, 0, nom, style);
        setCellDbl(r, 1, nb, style);
        setCellDbl(r, 2, total, style);
        setCellDbl(r, 3, Math.round(taux * 10.0) / 10.0, style);
        setCellStr(r, 4, critique ? "OUI ⚠" : "NON ✓", style);
    }

    private void createInfoCell(XSSFWorkbook wb, Row row, int col,
                                String label, String value, CellStyle valStyle) {
        XSSFRichTextString rts = new XSSFRichTextString();
        XSSFFont lf = wb.createFont();
        lf.setFontHeightInPoints((short) 8); lf.setBold(true);
        lf.setColor(IndexedColors.BLACK.getIndex());
        XSSFFont vf = wb.createFont();
        vf.setFontHeightInPoints((short) 11); vf.setBold(false);
        vf.setColor(IndexedColors.BLACK.getIndex());
        rts.append(label + "\n", lf);
        rts.append(value, vf);
        Cell c = row.createCell(col);
        c.setCellValue(rts);
        c.setCellStyle(valStyle);
    }

    private void setCellStr(Row row, int col, String val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val != null ? val : "—");
        c.setCellStyle(style);
    }

    private void setCellDbl(Row row, int col, double val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(style);
    }

    private void setCellNum(Row row, int col, Double val, CellStyle base, CellStyle alert,
                            java.util.function.Predicate<Double> alertPred) {
        Cell c = row.createCell(col);
        if (val != null) {
            c.setCellValue(val);
            c.setCellStyle(alertPred.test(val) ? alert : base);
        } else {
            c.setCellValue("—");
            c.setCellStyle(base);
        }
    }

    private long count(List<LigneRapport> list,
                       java.util.function.Predicate<LigneRapport> pred) {
        return list.stream().filter(pred).count();
    }

    private StatBlock calcStat(List<LigneRapport> list,
                               java.util.function.Function<LigneRapport, Double> getter) {
        double[] vals = list.stream().map(getter).filter(v -> v != null)
                .mapToDouble(Double::doubleValue).toArray();
        if (vals.length == 0) return new StatBlock(0, 0, 0);
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, sum = 0;
        for (double v : vals) { min = Math.min(min, v); max = Math.max(max, v); sum += v; }
        return new StatBlock(min, max, sum / vals.length);
    }

    record StatBlock(double min, double max, double avg) {}

    // ── Style factories ───────────────────────────────────────────

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 16);
        f.setColor(IndexedColors.BLACK.getIndex());
        s.setFont(f); s.setAlignment(HorizontalAlignment.LEFT);
        return s;
    }

    private CellStyle createSubTitleStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.BLACK.getIndex());
        f.setItalic(true);
        s.setFont(f); s.setAlignment(HorizontalAlignment.LEFT);
        return s;
    }

    private CellStyle createSectionStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11);
        f.setColor(IndexedColors.BLACK.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 9);
        f.setColor(IndexedColors.BLACK.getIndex());
        s.setFont(f); s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(s, BorderStyle.THIN, IndexedColors.BLACK.getIndex());
        return s;
    }

    private CellStyle createNormalStyle(XSSFWorkbook wb, boolean alert) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 9);
        if (alert) {
            f.setBold(true);
            f.setColor(IndexedColors.RED.getIndex());
        } else {
            f.setColor(IndexedColors.BLACK.getIndex());
        }
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN, IndexedColors.BLACK.getIndex());
        return s;
    }

    private CellStyle createAltRowStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 9);
        f.setColor(IndexedColors.BLACK.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        s.setFillPattern(FillPatternType.NO_FILL);
        s.setAlignment(HorizontalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN, IndexedColors.BLACK.getIndex());
        return s;
    }

    private CellStyle createInfoValueStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN, IndexedColors.BLACK.getIndex());
        return s;
    }

    private void setBorder(CellStyle s, BorderStyle bs, short colorIndex) {
        s.setBorderTop(bs); s.setBorderBottom(bs);
        s.setBorderLeft(bs); s.setBorderRight(bs);
        s.setTopBorderColor(colorIndex); s.setBottomBorderColor(colorIndex);
        s.setLeftBorderColor(colorIndex); s.setRightBorderColor(colorIndex);
    }
}