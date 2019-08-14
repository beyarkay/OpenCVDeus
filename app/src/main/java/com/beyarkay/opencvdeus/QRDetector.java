package com.beyarkay.opencvdeus;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


class QRDetector {
    private static final String TAG = QRDetector.class.toString();
    private Mat src;
    private double thresh;
    private double thresh_max;
    private double canny_thresh1;
    private double canny_thresh2;


    QRDetector(Mat src) {
        canny_thresh2 = 200.0;
        canny_thresh1 = 50.0;
        thresh_max = 255.0;
        thresh = 200;
        this.src = src;
    }

    public Mat getCorners() {
        double[] corners = new double[4];

        Mat grey = new Mat();
        Imgproc.cvtColor(this.src, grey, Imgproc.COLOR_RGB2GRAY);

        Mat thresholded = new Mat();
        Imgproc.threshold(grey, thresholded, this.thresh, this.thresh_max, Imgproc.THRESH_BINARY);

        Mat canny = new Mat();
        Imgproc.Canny(thresholded, canny, this.canny_thresh1, this.canny_thresh2);

        Mat heirarcy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(canny, contours, heirarcy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        List<Integer> marker_indexes = new ArrayList<>();
        List<Integer> markers1 = new ArrayList<>();
        List<Integer> markers2 = new ArrayList<>();
        List<Integer> markers3 = new ArrayList<>();
        List<Integer> markers4 = new ArrayList<>();
        List<MatOfPoint> contours1 = new ArrayList<>();
        List<MatOfPoint> contours2 = new ArrayList<>();
        List<MatOfPoint> contours3 = new ArrayList<>();
        List<MatOfPoint> contours4 = new ArrayList<>();
        List<MatOfPoint> goodContours = new ArrayList<>();
        int j;
        int count;
        for (int i = 0; i < contours.size(); i++) {
            j = i;
            count = 0;
//            System.out.println(Arrays.toString(heirarcy.get(j, 0)));

            /*
            Heirarch is an array of size Nx1 (col x row), where N is the number of contours found
            Each of the N elements is in turn a 4x1 double[]
            These elements are the ids of next, previous, parent, first_child, respectively
            a negative id means that that object doesn't exist
             */
            while (heirarcy.get(0, j)[2] != -1) {
                j = (int) heirarcy.get(0, j)[2];
                count++;
            }
            if (count >= 3) {
                marker_indexes.add(i);
            }
//            if (count >= 4) {
//                markers4.add(i);
//            } else if (count >= 3) {
//                markers3.add(i);
//            } else if (count >= 2) {
//                markers2.add(i);
//            } else if (count >= 1) {
//                markers1.add(i);
//            }

        }

        if (marker_indexes.size() < 3) {
            Log.e(TAG, "Number of detected markers is less than 3");
        } else {
//            marker_indexes = marker_indexes.subList(marker_indexes.size() - 3, marker_indexes.size());
        }


        for (int i = 0; i < marker_indexes.size(); i++) {
            goodContours.add(contours.get(marker_indexes.get(i)));
        }

//        for (int i = 0; i < marker_indexes.size(); i++) {
//            contours1.add(contours.get(marker_indexes.get(i)));
//        }
//        for (int i = 0; i < marker_indexes.size(); i++) {
//            contours2.add(contours.get(marker_indexes.get(i)));
//        }
//        for (int i = 0; i < marker_indexes.size(); i++) {
//            contours3.add(contours.get(marker_indexes.get(i)));
//        }
//        for (int i = 0; i < marker_indexes.size(); i++) {
//            contours4.add(contours.get(marker_indexes.get(i)));
//        }

        Mat withContours = Mat.zeros(this.src.size(), CvType.CV_8UC3);

        if (goodContours.size() == 0) {
            return null;
        }

//        for (int i = 0; i < contours1.size(); i++) {
//            Imgproc.drawContours(withContours, contours1, i, new Scalar(0, 0, 100));
//        }
//        for (int i = 0; i < contours2.size(); i++) {
//            Imgproc.drawContours(withContours, contours2, i, new Scalar(0, 0, 150));
//        }
//        for (int i = 0; i < contours3.size(); i++) {
//            Imgproc.drawContours(withContours, contours3, i, new Scalar(0, 0, 200));
//        }
//        for (int i = 0; i < contours4.size(); i++) {
//            Imgproc.drawContours(withContours, contours4, i, new Scalar(0, 0, 250));
//        }
        for (int i = 0; i < goodContours.size(); i++) {
            Imgproc.drawContours(withContours, goodContours, i, new Scalar(200, 150, 200));
        }
        return withContours;


//        return marker_indexes;
    }


}
