package com.example.occ_opencv;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Range;
import android.view.View;

import org.opencv.android.CameraActivity;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import android.os.AsyncTask;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessor extends CameraActivity {
    private static String LOADTAG = "Open_LOG";
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    /*public void processImage(byte[] imageData) {
        // Convertir el array de bytes a una matriz de OpenCV
        Mat inputImage = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_UNCHANGED);

        // Verificar si la matriz es válida
        if (inputImage.empty()) {
            Log.e("ImageProcessor", "Error al decodificar la imagen");
            return;
        }

        // Convertir la imagen a escala de grises
        Imgproc.cvtColor(inputImage, inputImage, Imgproc.COLOR_RGB2GRAY);

        // Obtener la ruta de salida para guardar la imagen
        String outputImagePath = getOutputImagePath();

        // Guardar la imagen en la ruta de salida
        saveImageToFile(inputImage, outputImagePath);

        // Liberar la memoria de la matriz
        inputImage.release();

        // Puedes agregar aquí código para mostrar la imagen en la interfaz de usuario
        // o cualquier otro método de visualización que prefieras
    }*/
    // Método para filtrar un canal de color con un límite dado
    private Mat filtrar(Mat channel, int limite) {
        Mat binary = new Mat();
        Mat binaryk = new Mat();
        Mat filteredFrame = new Mat();

        // Aplicar umbral
        Imgproc.threshold(channel, binary, limite, 255, Imgproc.THRESH_BINARY);

        // Aplicar k-means clustering
        //kMeanCluster(binary, binaryk);

        // Aplicar umbral de Otsu
        Imgproc.threshold(binaryk, filteredFrame, 127, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        // Liberar memoria de matrices temporales
        binary.release();
        binaryk.release();

        return filteredFrame;
    }

    // Método para realizar k-means clustering
    private void kMeanCluster(Mat src, Mat dst) {
        // Convertir la matriz a un frame con píxeles de 32 bits
        Mat floatFrame = new Mat();
        src.convertTo(floatFrame, CvType.CV_32F);

        // Definir los parámetros para k-means clustering
        TermCriteria termCriteria = new TermCriteria(TermCriteria.MAX_ITER + TermCriteria.EPS, 10, 1.0);
        int pixels = 2;

        // Aplicar k-means clustering
        Mat labels = new Mat();
        Mat centers = new Mat();
        Core.kmeans(floatFrame, pixels, labels, termCriteria, 10, Core.KMEANS_RANDOM_CENTERS, centers);

        // Convertir los centros a 8 bits sin signo
        centers.convertTo(centers, CvType.CV_8U);

        // Asignar los píxeles según las etiquetas
        Mat res = new Mat();
        centers.row((int) labels.get(0, 0)[0]).copyTo(res);

        Log.d("ImageProcessor", "Dimensiones antes de reshape: " + res.size());
        res = res.reshape(floatFrame.channels(), floatFrame.rows());



        // Convertir la matriz resultante a 8 bits sin signo
        //res.convertTo(dst, CvType.CV_8U);
    }



    /*public void processImage(byte[] imageData) {
        // Convertir el array de bytes a una matriz de OpenCV
        Mat inputImage = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_UNCHANGED);

        // Verificar si la matriz es válida
        if (inputImage.empty()) {
            Log.e("ImageProcessor", "Error al decodificar la imagen");
            return;
        }

        // Dividir la matriz en canales de color (Blue, Green, Red)
        List<Mat> channels = new ArrayList<>();
        Core.split(inputImage, channels);

        // Obtener la matriz del canal azul
        Mat blueChannel = channels.get(0);

        // Obtener la matriz del canal verde
        Mat greenChannel = channels.get(1);

        // Obtener la matriz del canal rojo
        Mat redChannel = channels.get(2);

        // Realizar la binarización en el canal azul
        Mat blueBinary = binarizeChannel(blueChannel);
        Log.d("ImageProcessor", "Matriz binaria del canal azul:\n" + blueBinary.dump());

        // Realizar la binarización en el canal rojo
        Mat redBinary = binarizeChannel(redChannel);
        Log.d("ImageProcessor", "Matriz binaria del canal rojo:\n" + redBinary.dump());


        // Liberar la memoria de la lista de matrices de canales
        for (Mat channel : channels) {
            channel.release();
        }


        inputImage.release();


        // Puedes agregar aquí código para mostrar los canales filtrados en la interfaz de usuario
        // o cualquier otro método de visualización que prefieras
    }*/
    public void processImage(byte[] imageData) {
        new ImageProcessingTask().execute(imageData);
    }
    private class ImageProcessingTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... params) {
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


            /*Mat redBinary = binarizeChannel(redChannel, 100);
            Mat blueBinary = binarizeChannel(blueChannel, 230);
            // Aplicar la umbralización al canal rojo
            //Imgproc.threshold(redChannel, redBinary, 100, 255, Imgproc.THRESH_BINARY);
            imprimirMatrizEnLogcat(LOADTAG, redChannel, " Red-CH");
            imprimirMatrizEnLogcat(LOADTAG,blueChannel," Blue-CH");
            imprimirMatrizEnLogcat(LOADTAG, redBinary, " Red-BIN");
            imprimirMatrizEnLogcat(LOADTAG, blueBinary, " Blue-BIN");

             */
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
                    imprimirMatrizEnLogcat(LOADTAG, redBinary, " Red-BIN");
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
                    imprimirMatrizEnLogcat(LOADTAG, blueBinary, " Blue-BIN");
                }
            };

            // Ejecutar las tareas AsyncTask en paralelo

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    redBinaryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    //blueBinaryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    //Log.d(LOADTAG, "Matriz binaria del canal azul:\n" );
                    Log.d("ImageProcessor", "Información del canal azul: " + blueChannel.toString());
                    Log.d("ImageProcessor", "Información del canal rojo: " + redChannel.toString());


                    //Log.d(LOADTAG, "Matriz binaria del canal rojo:\n" + redBinary.dump());
                    // Realiza cualquier operación en la interfaz de usuario aquí
                }
            });

            // Liberar la memoria de la lista de matrices de canales
            for (Mat channel : channels) {
                channel.release();
            }

            inputImage.release();

            return null;
        }



        private Mat binarizeChannel(Mat channel, int limite) {
            // Realizar la umbralización inicial en el canal
            Mat thresholded = new Mat();
            Imgproc.threshold(channel, thresholded, limite, 255, Imgproc.THRESH_BINARY);

            // Realizar la umbralización automática (Otsu) en el canal
            Imgproc.threshold(channel, thresholded, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

            return thresholded;
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
                System.out.println(tag + " - Fila " + i + ": " + rowStringBuilder.toString());
            }
            System.out.println(tag + " - Fila impresa ");
        }

    }
    /*private Mat binarizeChannel(Mat channel) {
        // Realizar la umbralización inicial en el canal
        Mat thresholded = new Mat();
        Imgproc.threshold(channel, thresholded, 128, 255, Imgproc.THRESH_BINARY);

        // Realizar la umbralización automática (Otsu) en el canal
        Imgproc.threshold(channel, thresholded, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        return thresholded;
    }
    */




    private String getOutputImagePath() {
        // Generar una ruta de archivo única para guardar la imagen
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                "/grayscale_image.jpg";
    }
}
