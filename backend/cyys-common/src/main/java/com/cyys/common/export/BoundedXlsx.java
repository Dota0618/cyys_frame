package com.cyys.common.export;

import com.cyys.common.web.ApiException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Semaphore;

/** 小型文本表格生成；调用方负责查询、字段白名单及授权，不读取模板或执行公式。 */
public final class BoundedXlsx {
    public static final int MAX_ROWS = 5000;
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final Semaphore WORKERS = new Semaphore(2);

    private BoundedXlsx() {}

    public static byte[] write(String sheetName, List<String> headers, List<List<String>> rows) throws IOException {
        return write(sheetName, headers, rows, MAX_ROWS);
    }

    public static byte[] write(String sheetName, List<String> headers, List<List<String>> rows, int maxRows) throws IOException {
        if (maxRows < 1 || maxRows > MAX_ROWS || headers.isEmpty() || headers.size() > 16 || rows.size() > maxRows) {
            throw new IllegalArgumentException("表格行列数超过同步导出上限");
        }
        long characters = validateCells(headers);
        for (var row : rows) {
            if (row.size() != headers.size()) throw new IllegalArgumentException("表格列数不一致");
            characters += validateCells(row);
            if (characters > 1_000_000) throw new ApiException(422, "导出内容过多，请缩小筛选范围");
        }
        if (!WORKERS.tryAcquire()) throw new ApiException(429, "导出繁忙，请稍后重试");
        try (var workbook = new XSSFWorkbook(); var buffer = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(sheetName);
            var textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            var headerStyle = workbook.createCellStyle();
            headerStyle.cloneStyleFrom(textStyle);
            var font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int index = 0; index <= rows.size(); index++) {
                var values = index == 0 ? headers : rows.get(index - 1);
                var row = sheet.createRow(index);
                for (int column = 0; column < values.size(); column++) {
                    var cell = row.createCell(column, CellType.STRING);
                    cell.setCellStyle(index == 0 ? headerStyle : textStyle);
                    cell.setCellValue(values.get(column) == null ? "" : values.get(column));
                }
            }
            for (int column = 0; column < headers.size(); column++) sheet.setColumnWidth(column, 22 * 256);
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, headers.size() - 1));
            // 完整生成后才提交 HTTP 响应，避免失败时返回半个成功文件。
            workbook.write(new OutputStream() {
                @Override public void write(int value) throws IOException {
                    requireCapacity(1); buffer.write(value);
                }
                @Override public void write(byte[] values, int offset, int length) throws IOException {
                    requireCapacity(length); buffer.write(values, offset, length);
                }
                private void requireCapacity(int length) throws IOException {
                    if (length > MAX_BYTES - buffer.size()) throw new IOException("Export output limit exceeded");
                }
            });
            return buffer.toByteArray();
        } finally { WORKERS.release(); }
    }

    private static long validateCells(List<String> cells) {
        long total = 0;
        for (var cell : cells) {
            if (cell == null) continue;
            if (cell.length() > 32767) throw new ApiException(422, "单元格内容过长，无法导出");
            total += cell.length();
        }
        return total;
    }
}
