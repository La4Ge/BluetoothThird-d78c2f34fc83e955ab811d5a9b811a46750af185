package com.example.bluetooththird;


import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

class Detect {


    public static void write(String filename, double[] x) throws IOException {
        BufferedWriter outputWriter = null;
        outputWriter = new BufferedWriter(new FileWriter(filename));
        for (int i = 0; i < x.length; i++) {
            outputWriter.write(String.valueOf(x[i]));
            outputWriter.newLine();
        }
        outputWriter.flush();
        outputWriter.close();
    }

    public static double[] xcorr(double[] a, double[] b) {
        int len = a.length;
        if (b.length > a.length)
            len = b.length;

        return xcorr(a, b, len - 1);

    }


    public static double[] xcorr(double[] a, double[] b, int maxlag) {
        double[] y = new double[2 * maxlag + 1];
        Arrays.fill(y, 0);

        for (int lag = b.length - 1, idx = maxlag - b.length + 1; lag > -a.length; lag--, idx++) {
            if (idx < 0)
                continue;

            if (idx >= y.length)
                break;

            int start = 0;
            if (lag < 0) {
                start = -lag;
            }

            int end = a.length - 1;
            if (end > b.length - lag - 1) {
                end = b.length - lag - 1;
            }

            for (int n = start; n <= end; n++) {
                y[idx] += a[n] * b[lag + n];
            }
        }

        return (y);
    }

    public static double isit(double[] recorded, double[] snipped) {
        try {
            write("/storage/emulated/0/AUFNAHME.txt", recorded);

        } catch (Exception e) {

        }
        try {
            write("/storage/emulated/0/SCHNIPSEL.txt", snipped);

        } catch (Exception e) {

        }


        double[] mycorr = xcorr(recorded, snipped);

        try {
            write("/storage/emulated/0/XCORR.txt", mycorr);

        } catch (Exception e) {

        }


        double largest_counter = 0;
        double secondlargest_counter = 0;

        double largestCorr = 0;
        double secondlargestCorr = 0;

        for (int s = 0; s < mycorr.length; s++) {

            if (mycorr[s] > largestCorr) {
                if (s > Math.abs(largest_counter + 5000)) {
                    secondlargestCorr = largestCorr;
                    secondlargest_counter = largest_counter;
                }
                largestCorr = mycorr[s];
                largest_counter = s;
            } else if (mycorr[s] > secondlargestCorr) {
                if (s > Math.abs(largest_counter + 5000)) {
                    secondlargestCorr = mycorr[s];
                    secondlargest_counter = s;
                }
            }
        }
        return Math.abs(largest_counter - secondlargest_counter);
    }
}
