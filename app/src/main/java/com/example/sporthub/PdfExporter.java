package com.example.sporthub;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PdfExporter {

    private static final int PAGE_WIDTH = 595;  // A4 width in points
    private static final int PAGE_HEIGHT = 842; // A4 height in points
    private static final int MARGIN = 50;
    private static final int LINE_HEIGHT = 25;

    private Context context;
    private ArrayList<DiaRutina> diasRutina;

    public PdfExporter(Context context, ArrayList<DiaRutina> diasRutina) {
        this.context = context;
        this.diasRutina = diasRutina;
    }

    public void exportarPdf() {
        // Crear documento PDF
        PdfDocument pdfDocument = new PdfDocument();

        // Configurar paints (estilos de texto)
        Paint paintTitle = new Paint();
        paintTitle.setTextSize(24);
        paintTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintTitle.setColor(android.graphics.Color.BLACK);

        Paint paintHeader = new Paint();
        paintHeader.setTextSize(18);
        paintHeader.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintHeader.setColor(android.graphics.Color.rgb(41, 128, 185)); // Azul

        Paint paintNormal = new Paint();
        paintNormal.setTextSize(14);
        paintNormal.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paintNormal.setColor(android.graphics.Color.BLACK);

        Paint paintSubtext = new Paint();
        paintSubtext.setTextSize(12);
        paintSubtext.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        paintSubtext.setColor(android.graphics.Color.GRAY);

        Paint paintLine = new Paint();
        paintLine.setColor(android.graphics.Color.LTGRAY);
        paintLine.setStrokeWidth(1);

        // Variables de posición
        int currentY = MARGIN + 30;
        int pageNumber = 1;

        // Crear primera página
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Título del documento
        canvas.drawText("Mi Rutina de Entrenamiento", MARGIN, currentY, paintTitle);
        currentY += LINE_HEIGHT + 10;

        // Fecha de generación
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        canvas.drawText("Generado el: " + sdf.format(new Date()), MARGIN, currentY, paintSubtext);
        currentY += LINE_HEIGHT + 20;

        // Línea separadora
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, paintLine);
        currentY += 20;

        // Recorrer todos los días y ejercicios
        for (DiaRutina dia : diasRutina) {
            // Verificar si necesitamos una nueva página
            if (currentY > PAGE_HEIGHT - MARGIN - 100) {
                pdfDocument.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                currentY = MARGIN + 30;
            }

            // Nombre del día (header)
            String nombreDia = capitalizarPrimeraLetra(dia.getNombreDia());
            canvas.drawText(nombreDia, MARGIN, currentY, paintHeader);
            currentY += LINE_HEIGHT + 5;

            // Línea debajo del día
            canvas.drawLine(MARGIN, currentY, MARGIN + 150, currentY, paintLine);
            currentY += 15;

            // Ejercicios del día
            ArrayList<Ejercicio> ejercicios = dia.getEjercicios();

            if (ejercicios.isEmpty()) {
                canvas.drawText("   No hay ejercicios programados", MARGIN + 20, currentY, paintSubtext);
                currentY += LINE_HEIGHT;
            } else {
                for (int i = 0; i < ejercicios.size(); i++) {
                    Ejercicio ejercicio = ejercicios.get(i);

                    // Verificar espacio para ejercicio
                    if (currentY > PAGE_HEIGHT - MARGIN - 60) {
                        pdfDocument.finishPage(page);
                        pageNumber++;
                        pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                        page = pdfDocument.startPage(pageInfo);
                        canvas = page.getCanvas();
                        currentY = MARGIN + 30;
                    }

                    // Número y nombre del ejercicio
                    String textoEjercicio = "   " + (i + 1) + ". " + ejercicio.getNombre();
                    canvas.drawText(textoEjercicio, MARGIN + 20, currentY, paintNormal);
                    currentY += LINE_HEIGHT - 5;

                    // Grupo muscular
                    String grupoMuscular = "      Grupo: " + capitalizarPrimeraLetra(ejercicio.getGrupoMuscular());
                    canvas.drawText(grupoMuscular, MARGIN + 20, currentY, paintSubtext);
                    currentY += LINE_HEIGHT;
                }
            }

            currentY += 15; // Espacio entre días
        }

        // Finalizar última página
        pdfDocument.finishPage(page);

        // Guardar el PDF
        guardarPdf(pdfDocument);
    }

    private void guardarPdf(PdfDocument pdfDocument) {
        // Crear nombre del archivo con fecha
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String nombreArchivo = "Rutina_" + timestamp + ".pdf";

        // Obtener directorio de descargas
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, nombreArchivo);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            Toast.makeText(context, "PDF generado exitosamente", Toast.LENGTH_SHORT).show();

            // Abrir el PDF automáticamente
            abrirPdf(file);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al guardar el PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void abrirPdf(File file) {
        try {
            Uri uri;

            // Para Android 7.0 (API 24) y superior, usar FileProvider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".fileprovider",
                        file);
            } else {
                uri = Uri.fromFile(file);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Verificar que hay una app que pueda abrir PDFs
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context,
                        "No hay aplicación para abrir PDFs. PDF guardado en: " + file.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context,
                    "PDF guardado en: " + file.getAbsolutePath() + "\nNo se pudo abrir automáticamente.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String capitalizarPrimeraLetra(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}