package com.teensconf.yandex_microservice.yandex;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;
import com.teensconf.yandex_microservice.dto.ExportRegistrationDTO;

@Slf4j
@Component
public class SheetFormatter {

    public byte[] buildXlsxWithTwoSheets(List paidRegistrations, List allAttempts) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            String[] columns = {"id","Имя","Фамилия","Email","Дата рождения","Телефон","Город","Telegram","Нужно жилье","Церковь","Роль","ФИО родителя","Телефон родителя","ID оплаты","Ссылка на чек"};

            CellStyle linkStyle = workbook.createCellStyle();
            Font linkFont = workbook.createFont();
            linkFont.setUnderline(Font.U_SINGLE);
            linkFont.setColor(IndexedColors.BLUE.getIndex());
            linkStyle.setFont(linkFont);

            // Лист с оплаченными регистрациями
            Sheet paidSheet = workbook.createSheet("PaidRegistrations");
            Row paidHeader = paidSheet.createRow(0);
            for (int i = 0; i < columns.length; i++) paidHeader.createCell(i).setCellValue(columns[i]);

            int rowNum = 1;
            for (Object r : paidRegistrations) {
                if (r instanceof ExportRegistrationDTO) {
                    ExportRegistrationDTO dto = (ExportRegistrationDTO) r;
                    if (dto.getReceiptId() == null) continue;
                    Row row = paidSheet.createRow(rowNum++);
                    int c = 0;
                    row.createCell(c++).setCellValue(dto.getId() == null ? "" : dto.getId().toString());
                    row.createCell(c++).setCellValue(dto.getFirstName() == null ? "" : dto.getFirstName());
                    row.createCell(c++).setCellValue(dto.getLastName() == null ? "" : dto.getLastName());
                    row.createCell(c++).setCellValue(dto.getEmail() == null ? "" : dto.getEmail());
                    row.createCell(c++).setCellValue(dto.getBirthDate() == null ? "" : dto.getBirthDate());
                    row.createCell(c++).setCellValue(dto.getPhone() == null ? "" : dto.getPhone());
                    row.createCell(c++).setCellValue(dto.getCity() == null ? "" : dto.getCity());
                    row.createCell(c++).setCellValue(dto.getTelegram() == null ? "" : dto.getTelegram());
                    row.createCell(c++).setCellValue(dto.getNeedAccommodation() == null ? "" : dto.getNeedAccommodation().toString());
                    row.createCell(c++).setCellValue(dto.getChurch() == null ? "" : dto.getChurch());
                    row.createCell(c++).setCellValue(dto.getRole() == null ? "" : dto.getRole());
                    row.createCell(c++).setCellValue(dto.getParentFullName() == null ? "" : dto.getParentFullName());
                    row.createCell(c++).setCellValue(dto.getParentPhone() == null ? "" : dto.getParentPhone());
                    row.createCell(c++).setCellValue(dto.getReceiptId() == null ? "" : dto.getReceiptId().toString());
                    String url = dto.getYandexDiskUrl() != null ? dto.getYandexDiskUrl() : "";
                    row.createCell(c++).setCellValue(url);
                }
            }

            for (int i = 0; i < columns.length; i++) paidSheet.autoSizeColumn(i);

            // Лист с попытками регистраций
            Sheet attemptsSheet = workbook.createSheet("Attempts");
            Row attemptsHeader = attemptsSheet.createRow(0);
            for (int i = 0; i < columns.length; i++) attemptsHeader.createCell(i).setCellValue(columns[i]);
            rowNum = 1;
            for (Object r : allAttempts) {
                if (r instanceof ExportRegistrationDTO) {
                    ExportRegistrationDTO dto = (ExportRegistrationDTO) r;
                    if (dto.getReceiptId() != null) continue;
                    Row row = attemptsSheet.createRow(rowNum++);
                    int c = 0;
                    row.createCell(c++).setCellValue(dto.getId() == null ? "" : dto.getId().toString());
                    row.createCell(c++).setCellValue(dto.getFirstName() == null ? "" : dto.getFirstName());
                    row.createCell(c++).setCellValue(dto.getLastName() == null ? "" : dto.getLastName());
                    row.createCell(c++).setCellValue(dto.getEmail() == null ? "" : dto.getEmail());
                    row.createCell(c++).setCellValue(dto.getBirthDate() == null ? "" : dto.getBirthDate());
                    row.createCell(c++).setCellValue(dto.getPhone() == null ? "" : dto.getPhone());
                    row.createCell(c++).setCellValue(dto.getCity() == null ? "" : dto.getCity());
                    row.createCell(c++).setCellValue(dto.getTelegram() == null ? "" : dto.getTelegram());
                    row.createCell(c++).setCellValue(dto.getNeedAccommodation() == null ? "" : dto.getNeedAccommodation().toString());
                    row.createCell(c++).setCellValue(dto.getChurch() == null ? "" : dto.getChurch());
                    row.createCell(c++).setCellValue(dto.getRole() == null ? "" : dto.getRole());
                    row.createCell(c++).setCellValue(dto.getParentFullName() == null ? "" : dto.getParentFullName());
                    row.createCell(c++).setCellValue(dto.getParentPhone() == null ? "" : dto.getParentPhone());
                    row.createCell(c++).setCellValue(dto.getReceiptId() == null ? "" : "");
                    String url = dto.getYandexDiskUrl() != null ? dto.getYandexDiskUrl() : "";
                    row.createCell(c++).setCellValue(url);
                }
            }

            for (int i = 0; i < columns.length; i++) attemptsSheet.autoSizeColumn(i);

            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}