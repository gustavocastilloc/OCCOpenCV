package com.example.occ_opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Range;
import android.view.TextureView;
import android.view.View;

import org.opencv.android.CameraActivity;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import android.os.AsyncTask;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ImageProcessor extends CameraActivity {
    private static String LOADTAG = "Open_LOG";
    private TextView MessageView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }



    public void processImage(byte[] imageData, Handler handler) {
        new ImageProcessingTask(handler).execute(imageData);
    }
    private class ImageProcessingTask extends AsyncTask<byte[], Void,  String> {
        private String textoMensaje="";
        private Handler mHandler;

        public ImageProcessingTask(Handler handler) {
            this.mHandler = handler;
        }
        @Override
        protected String doInBackground(byte[]... params) {
            byte[] imageData = params[0];

            // Convertir el array de bytes a una matriz de OpenCV
            Mat inputImage = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_UNCHANGED);

            // Verificar si la matriz es válida
            if (inputImage.empty()) {
                Log.e(LOADTAG, "Error al decodificar la imagen");
                return null;
            }

            // Dividir la matriz en canales de color (Blue, Green, Red)
            List<Mat> channels = new ArrayList<>();
            Core.split(inputImage, channels);

            // Obtener la matriz del canal azul
            Mat blueChannel = channels.get(0);

            // Obtener la matriz del canal rojo
            Mat redChannel = channels.get(2);


            Mat redBinary = binarizeChannel(redChannel, 60);
            Mat blueBinary = binarizeChannel(blueChannel, 80);

            // Aplicar la umbralización al canal rojo
            //Imgproc.threshold(redChannel, redBinary, 100, 255, Imgproc.THRESH_BINARY);
            //imprimirMatrizEnLogcat(LOADTAG, redChannel, " Red-CH");
            //imprimirMatrizEnLogcat(LOADTAG,blueChannel," Blue-CH");
            //imprimirMatrizEnLogcat(LOADTAG, redBinary, " Red-BIN");
            //imprimirMatrizEnLogcat(LOADTAG, blueBinary, " Blue-BIN");
            Mat redROI=recortarROI(redBinary,100);
            Mat blueROI= recortarROI(blueBinary,100);
            //saveBinaryImage(blueBinary, "imgBLUEbinary");
            //saveBinaryImage(redBinary, "imgREDbinary");
            int percentageRed= 0;
            int percentageBlue=0;
            int simbolo=0;
            if (redROI != null && blueROI != null) {
                System.out.println("Cantidad de filas en redROI: " + redROI.rows());
                System.out.println("Cantidad de Columnas en redROI: " + redROI.cols());
                double[] porcentajesRojo = porcentajeValoresPixeles(redROI);
                double[] porcentajesAzul = porcentajeValoresPixeles(blueROI);
                System.out.println("%Fondo RedROI="+porcentajesRojo[0]+" %Objeto RedROI="+porcentajesRojo[1]);
                System.out.println("%Fondo BlueROI="+porcentajesAzul[0]+" %Objeto BlueROI="+porcentajesAzul[1]);
                percentageRed = compensarValor(porcentajesRojo[1]);
                percentageBlue = compensarValor(porcentajesAzul[1]);
                System.out.println("%RED Obj: "+percentageRed);
                System.out.println("%BLUE Obj: "+percentageBlue);
                simbolo=decodificarSimbolo(percentageRed, percentageBlue);
                System.out.println("Simbolo="+simbolo);
                // Convierte el número entero a una cadena de texto
                textoMensaje = String.valueOf(simbolo);

                //saveBinaryImage(blueROI, "imgBlueROI");
                //saveBinaryImage(redROI, "imgRedROI");
            } else {
                // Manejar el caso en el que las matrices recortadas son nulas
                System.out.println("¡Error al recortar las imágenes!");
            }
            /*
            // Crear una tarea AsyncTask para la umbralización del canal rojo
            AsyncTask<Void, Void, Mat> redBinaryTask = new AsyncTask<Void, Void, Mat>() {
                @Override
                protected Mat doInBackground(Void... voids) {
                    return binarizeChannel(redChannel, 100);
                }

                @Override
                protected void onPostExecute(Mat redBinary) {
                    super.onPostExecute(redBinary);
                    Log.d(LOADTAG, "Inicio de onPostExecute");

                    //imprimirMatrizEnLogcat(LOADTAG, redBinary, " Red-BIN");
                    Log.d(LOADTAG, "Fin de onPostExecute");
                }
            };

            // Crear una tarea AsyncTask para la umbralización del canal azul
            AsyncTask<Void, Void, Mat> blueBinaryTask = new AsyncTask<Void, Void, Mat>() {
                @Override
                protected Mat doInBackground(Void... voids) {
                    return binarizeChannel(blueChannel, 230);
                }

                @Override
                protected void onPostExecute(Mat blueBinary) {
                    super.onPostExecute(blueBinary);

                    //imprimirMatrizEnLogcat(LOADTAG, blueBinary, " Blue-BIN");
                }
            };
            */
            // Ejecutar las tareas AsyncTask en paralelo

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //redBinaryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    //blueBinaryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    //Log.d(LOADTAG, "Matriz binaria del canal azul:\n" );

                    Log.d("ImageProcessor", "Información del canal azul: " + blueChannel.toString());
                    Log.d("ImageProcessor", "Información del canal rojo: " + redChannel.toString());


                    System.out.println("SIMBOLO THREAD="+textoMensaje);


                    //Log.d(LOADTAG, "Matriz binaria del canal rojo:\n" + redBinary.dump());
                    // Realiza cualquier operación en la interfaz de usuario aquí
                }
            });

            // Liberar la memoria de la lista de matrices de canales
            for (Mat channel : channels) {
                channel.release();
            }

            inputImage.release();

            return textoMensaje;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Enviar un mensaje al Handler con el texto resultante
            if (mHandler != null) {
                Message message = mHandler.obtainMessage(0, result);
                mHandler.sendMessage(message);
            }
        }

        private void saveBinaryImage(Mat binaryMat, String nombre) {
            // Crea un Bitmap a partir de la matriz binaria
            Bitmap bitmap = Bitmap.createBitmap(binaryMat.cols(), binaryMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(binaryMat, bitmap);
            File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File file = new File(picturesDirectory, UUID.randomUUID().toString() + "_" + nombre + ".jpg");
            OutputStream outputStream = null;

            // Guarda el Bitmap como archivo de imagen utilizando FileOutputStream
            try  {
                outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                System.out.println("Imagen binaria guardada exitosamente en: " + Environment.getExternalStorageDirectory().getPath());
            } catch (IOException e) {
                System.err.println("Error al guardar la imagen binaria: " + e.getMessage());
            } finally {
                // Libera la memoria de la matriz binaria y el Bitmap
                binaryMat.release();
                bitmap.recycle();
            }
        }

        public Mat kMeanCluster(Mat frame) {
            Mat floatFrame = new Mat();
            frame.convertTo(floatFrame, CvType.CV_32F); // Convierte la imagen a 32 bits de punto flotante
            System.out.println("Cantidad de filas en framekmean: " +frame.rows());
            System.out.println("Cantidad de Columnas en framekmean: " +frame.cols());
            TermCriteria criteria = new TermCriteria(TermCriteria.MAX_ITER + TermCriteria.EPS, 10, 1.0);
            int pixels = 2;

            Mat label = new Mat();
            Mat center = new Mat();
            Core.kmeans(floatFrame, pixels, label, criteria, 10, Core.KMEANS_RANDOM_CENTERS, center);

            System.out.println("Cantidad de filas en center: " +center.rows());
            System.out.println("Cantidad de Columnas en center: " +center.cols());
            center.convertTo(center, CvType.CV_8U);


            System.out.println("Cantidad de filas en label: " +label.rows());
            System.out.println("Cantidad de Columnas en label: " +label.cols());
            System.out.println("Cantidad de filas en floatFrame: " +floatFrame.rows());
            System.out.println("Cantidad de columnas en floatFrame: " +floatFrame.cols());
            Mat res = new Mat(frame.rows(), frame.cols(), center.type());
            for (int i = 0; i < frame.rows(); i++) {
                center.rowRange(0, 1).copyTo(res.rowRange(i, i + 1));
            }

            System.out.println("Cantidad de filas en res: " + res.rows());
            System.out.println("Cantidad de Columnas en res: " + res.cols());

            //imprimirMatrizEnLogcat(LOADTAG, res, "CENTER");

            return res;
        }





        private Mat binarizeChannel(Mat channel, int thresholdValue) {
            // Convertir la imagen a escala de grises si es una imagen en color
            Imgproc.GaussianBlur(channel, channel, new Size(5, 5), 0);
            System.out.println("Cantidad de filas en Channel: " +channel.rows());
            System.out.println("Cantidad de Columnas en Channel: " +channel.cols());
            // Realizar la umbralización inicial en el canal
            Mat thresholded = new Mat();
            Mat filteredFrame = new Mat();
            Imgproc.threshold(channel, thresholded, thresholdValue, 255, Imgproc.THRESH_BINARY);
            int kernelSize = 3;
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
            Imgproc.erode(thresholded, thresholded, kernel);
            Imgproc.dilate(thresholded, thresholded, kernel);
            System.out.println("Cantidad de filas en thresholded: " +thresholded.rows());
            System.out.println("Cantidad de Columnas en thresholded: " +thresholded.cols());
            // Realizar la umbralización automática (Otsu) en el canal después de aplicar kMeans
            Mat binaryk = kMeanCluster(thresholded);
            Imgproc.threshold(binaryk, filteredFrame, 127, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
            //imprimirMatrizEnLogcat(LOADTAG,binaryk,"binaryk");
            System.out.println("Cantidad de filas en binaryk: " +binaryk.rows());
            System.out.println("Cantidad de Columnas en binaryk: " +binaryk.cols());
            thresholded.release();
            binaryk.release();
            System.out.println("Cantidad de filas en filteredFrame: " +filteredFrame.rows());
            System.out.println("Cantidad de Columnas en filteredFrame: " +filteredFrame.cols());

            return filteredFrame;
        }

        private Mat recortarROI(Mat imagenBinaria, double areaMinima) {
            // Encontrar los límites superior, inferior, izquierdo y derecho de las franjas blancas
            List<MatOfPoint> contornos = new ArrayList<>();
            Imgproc.findContours(imagenBinaria, contornos, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Filtrar contornos pequeños (área menor a 'areaMinima')
            List<MatOfPoint> contornosFiltrados = new ArrayList<>();
            for (MatOfPoint contorno : contornos) {
                if (Imgproc.contourArea(contorno) >= areaMinima) {
                    contornosFiltrados.add(contorno);
                }
            }

            if (contornosFiltrados.isEmpty()) {
                return null;
            }

            int min_y = Integer.MAX_VALUE;
            int max_y = Integer.MIN_VALUE;
            int min_x = Integer.MAX_VALUE;
            int max_x = Integer.MIN_VALUE;

            for (MatOfPoint contorno : contornosFiltrados) {
                Rect boundingRect = Imgproc.boundingRect(contorno);
                int x = boundingRect.x;
                int y = boundingRect.y;
                int w = boundingRect.width;
                int h = boundingRect.height;

                min_y = Math.min(min_y, y);
                max_y = Math.max(max_y, y + h);
                min_x = Math.min(min_x, x);
                max_x = Math.max(max_x, x + w);
            }

            // Ordenar los contornos de izquierda a derecha según su posición en el eje x
            contornosFiltrados.sort(Comparator.comparingInt(c -> Imgproc.boundingRect(c).x));

            // Obtener el índice de inicio de la segunda franja y el índice de inicio de la última franja
            if (contornosFiltrados.size() < 3) {
                return null;
            }

            int segundoInicioX = Imgproc.boundingRect(contornosFiltrados.get(1)).x;
            int ultimoInicioX = Imgproc.boundingRect(contornosFiltrados.get(contornosFiltrados.size() - 1)).x;

            // Recortar la imagen original utilizando los límites encontrados
            Rect roiRect = new Rect(segundoInicioX, min_y, ultimoInicioX - segundoInicioX, max_y - min_y);
            Mat imagenRecortada = new Mat(imagenBinaria, roiRect);

            if (imagenRecortada.rows() == 0 || imagenRecortada.cols() == 0) {
                System.out.println("imagen recortada tiene 0 filas o columnas");
                return null;
            }

            return imagenRecortada;
        }
        private double[] porcentajeValoresPixeles(Mat imagenBinaria) {
            // Contar la cantidad de píxeles con valor 0 (fondo) y 255 (objeto)
            //imprimirMatrizEnLogcat(LOADTAG,imagenBinaria,"%Matriz ");
            int totalPixeles = imagenBinaria.rows() * imagenBinaria.cols();
            int pixelesFondo = totalPixeles - Core.countNonZero(imagenBinaria);
            int pixelesObjeto = Core.countNonZero(imagenBinaria);
            System.out.println("TotalPixeles="+totalPixeles);
            System.out.println("pixelesFondo="+pixelesFondo);
            System.out.println("pixelesObjeto="+pixelesObjeto);

            // Calcular los porcentajes
            double porcentajeFondo = (double) pixelesFondo / totalPixeles * 100;
            double porcentajeObjeto = (double) pixelesObjeto / totalPixeles * 100;

            return new double[]{porcentajeFondo, porcentajeObjeto};
        }
        public int compensarValor(double porcentaje) {
            int valorCompensado = -1;

            if (porcentaje >= 10 && porcentaje < 30) {
                valorCompensado = 20;
            } else if (porcentaje >= 30 && porcentaje < 50) {
                valorCompensado = 40;
            } else if (porcentaje >= 50 && porcentaje < 70) {
                valorCompensado = 60;
            } else if (porcentaje >= 70 && porcentaje < 90) {
                valorCompensado = 80;
            }

            return valorCompensado;
        }
        public int decodificarSimbolo(int valorRed, int valorBlue) {
            int s = -1;

            // Compensar los valores
            valorRed = compensarValor(valorRed);
            valorBlue = compensarValor(valorBlue);

            // Calcular el resultado según la fórmula
            if (valorRed > 0 && valorBlue > 0) {
                s = (valorRed / 5) + (valorBlue / 20) - 5;
            }

            return s;
        }



        // Método para imprimir una matriz en Logcat
        private void imprimirMatrizEnLogcat(String tag, Mat matriz, String tipo) {
            int rows = matriz.rows();
            int cols = matriz.cols();

            // Imprimir la matriz en Logcat
            for (int i = 0; i < rows; i++) {
                StringBuilder rowStringBuilder = new StringBuilder();
                for (int j = 0; j < cols; j++) {
                    // Obtener el valor del píxel en la posición (i, j)
                    double pixelValue = matriz.get(i, j)[0];
                    // Agregar el valor a la cadena de la fila
                    rowStringBuilder.append(pixelValue).append(" ");
                }
                // Imprimir la fila en Logcat
                //Log.e(LOADTAG, "Fila"+tipo + i + ": " + rowStringBuilder.toString());
                System.out.println(tag + " - Fila " +tipo + i + ": " + rowStringBuilder.toString());
            }
            System.out.println(tag + " - MATRIZ impresa ");
        }

    }
    /*private Mat binarizeChannel(Mat channel, int limite) {
        // Aplicar suavizado gaussiano
        Imgproc.GaussianBlur(channel, channel, new Size(5, 5), 0);
        // Realizar la umbralización inicial en el canal
        Mat thresholded = new Mat();
        Imgproc.threshold(channel, thresholded, limite, 255, Imgproc.THRESH_BINARY);
        // Aplicar erosión y dilatación
        int kernelSize = 3;
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
        Imgproc.erode(thresholded, thresholded, kernel);
        Imgproc.dilate(thresholded, thresholded, kernel);
        // Realizar la umbralización automática (Otsu) en el canal
        Imgproc.threshold(channel, thresholded, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        return thresholded;
    }*/







    private String getOutputImagePath() {
        // Generar una ruta de archivo única para guardar la imagen
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                "/grayscale_image.jpg";
    }
}
