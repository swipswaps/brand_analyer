package csci576;

import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.openimaj.image.renderer.MBFImageRenderer;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.xuggle.XuggleVideo;

import com.lowagie.text.Rectangle;

import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.shape.Shape;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.processing.motion.GridMotionEstimator;
import org.openimaj.video.processing.motion.MotionEstimator;
import org.openimaj.video.processing.motion.MotionEstimatorAlgorithm;
import org.openimaj.video.processing.shotdetector.CombiShotDetector;
import org.openimaj.video.processing.shotdetector.HistogramVideoShotDetector;
import org.openimaj.video.processing.shotdetector.LocalHistogramVideoShotDetector;
import org.openimaj.video.processing.shotdetector.ShotBoundary;
import org.openimaj.video.processing.shotdetector.ShotDetectedListener;
import org.openimaj.video.processing.shotdetector.VideoKeyframe;
import org.openimaj.video.timecode.VideoTimecode;
import org.openimaj.video.translator.FImageToMBFImageVideoTranslator;
import org.openimaj.video.translator.MBFImageToFImageVideoTranslator;
import org.openimaj.demos.video.VideoShotDetectorVisualisation;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;


/**
 * 
 * CSCI576 FINAL PROJECT
 *
 */
public class App {
	
	/*
	 * Method that will plot the calculated motion vectors between frames
	 * 
	 * @param video The input video file to process
	 * 
	 */
	
	public static void displayMotionVectors(XuggleVideo video) {
		
		final MotionEstimator me = new GridMotionEstimator(
				new MBFImageToFImageVideoTranslator(video),
				new MotionEstimatorAlgorithm.PHASE_CORRELATION(), 10, 10, true);

		final VideoDisplay<MBFImage> vd = VideoDisplay.createVideoDisplay(
				new FImageToMBFImageVideoTranslator(me));
		vd.addVideoListener(new VideoDisplayListener<MBFImage>()
		{
			@Override
			public void afterUpdate(VideoDisplay<MBFImage> display)
			{
			}

			@Override
			public void beforeUpdate(MBFImage frame)
			{
				for (final Point2d p : me.motionVectors.keySet())
				{
					final Point2d p2 = me.motionVectors.get(p);
					frame.drawLine((int) p.getX(), (int) p.getY(),
							(int) (p.getX() + p2.getX()),
							(int) (p.getY() + p2.getY()),
							2, new Float[] { 1f, 0f, 0f });
				}
			}
		});
		
	}
	
	/*
	 * Method that will generate a histogram from an input frame. 
	 * 
	 * @param frame The input frame to process
	 * 
	 */
	
	public static MultidimensionalHistogram generateHistogram(MBFImage frame, int nBins) {
		
		HistogramModel model = new HistogramModel(8, 8, 8);
		model.estimateModel(frame);
		MultidimensionalHistogram histogram = model.histogram;
		
		return histogram;
		
	}
	
	/*
	 * Method that will calculate the MSD of a current frame and a previous frame 
	 * 
	 * @param current The current frame
	 * @param last The previous frame
	 * 
	 */
	
	public static float meanSquareDifference(FImage current, FImage last) {
		
		// compute the squared difference from the last frame
		float val = 0;
		for (int y = 0; y < current.height; y++) {
			for (int x = 0; x < current.width; x++) {
				final float diff = (current.pixels[y][x] - last.pixels[y][x]);
				val += diff * diff;
			}
		}
		
		float meanVal = val / (current.height * current.width);
		
		return meanVal;
		
	}
	
	/*
	 * Method that will determine where the shot boundaries.
	 * It uses MSD and histograms to compare to a threshold.
	 * 
	 * @param video The input video file to process. 
	 * @param threshold The threshold to determine a shot boundary.
	 * @param minFrames The minimum allowable frames per shot. 
	 * 
	 */
	
	public static List<Float> shotBoundaryDetector(XuggleVideo video, float threshold, int minFrames){
		
		// Get the first frame
		MBFImage lastFrame = video.getNextFrame();
		FImage last = lastFrame.flatten();
		
		// Initialize frame count
		float lastFrameNo = 0;
		float currentFrameNo = 1;
		
		// Initialize histogram
		int nBins = video.getWidth();
		MultidimensionalHistogram lastHistogram = generateHistogram(lastFrame, nBins);
		
		// Initialize output list
		List<Float> outputList = new ArrayList<Float>();
		outputList.add(lastFrameNo);
		
		// Iterate through the frames
		for (final MBFImage currentFrame : video) {
			final FImage current = currentFrame.flatten();
			
			float meanVal = meanSquareDifference(current, last);
			
			MultidimensionalHistogram currentHistogram = generateHistogram(currentFrame, nBins);
			double distanceScore = currentHistogram.compare(lastHistogram, DoubleFVComparison.EUCLIDEAN);
			
			//System.out.println("Current Histogram: " + currentHistogram.toString());
			//System.out.println("Last Histogram: " + lastHistogram.toString());
			
			// Might need adjust threshold:
			if (meanVal > threshold && distanceScore > threshold) {
				
				float lastShotStart = outputList.get(outputList.size() - 1);
				
				System.out.println("Frames in between: " + (lastFrameNo - lastShotStart));
				
				if ((lastFrameNo - lastShotStart) >  minFrames) {
				
					System.out.println("New Shot detected");
					DisplayUtilities.displayName(current, "debug: Current Frame");
					DisplayUtilities.displayName(last, "debug: Last Frame");					
					outputList.add(lastFrameNo);
					System.out.println("List: " + outputList.toString());
					
				}
				
			}
			
			System.out.println("From frame " + lastFrameNo + ", to " + currentFrameNo + ", meanVal = " + meanVal + ", histogramScore = " + distanceScore);					
			
			// Set the current frame to the last frame
			last = current;
			lastFrame = currentFrame;
			lastHistogram = currentHistogram;
			
			lastFrameNo ++;
			currentFrameNo ++;
			
		}
						
		return outputList;
		
	}
	

    public static void main( String[] args ) {

    	XuggleVideo video = new XuggleVideo(new File("/Users/skalli93/Desktop/USC Documents/CSCI576/finalProject/dataset/Videos/data_test1_cmp.avi"));
    	
    	//VideoDisplay<MBFImage> display = VideoDisplay.createVideoDisplay(video);
    	
    	List<Float> frameBoundary = shotBoundaryDetector(video, 0.07f, 5);
		
    	System.out.println(frameBoundary);
    	
    	//new VideoFeatureExtraction();
    	
    	
//    	for (MBFImage mbfImage : video) {
//    	    DisplayUtilities.displayName(mbfImage.process(new CannyEdgeDetector()), "videoFrames");
//    	}
    	
    }
}