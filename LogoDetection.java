package csci576;

import java.util.List;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.LinkedList;
import java.nio.file.Files;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;  
import java.nio.file.Paths;

import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.protobuf.ByteString;


public class LogoDetection 
{
      public static String detectLogos(String path, PrintStream out) throws Exception, IOException {
           List<AnnotateImageRequest> requests = new ArrayList<>();
           List<String> logos = new ArrayList<>();
           
            ByteString imgBytes = ByteString.readFrom(new FileInputStream(path));

            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
            AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
            requests.add(request);

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
              BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
              List<AnnotateImageResponse> responses = response.getResponsesList();
              String foundLogo = null;
              for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                  out.printf("Error: %s\n", res.getError().getMessage());
                  return null;
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
                    foundLogo = annotation.getDescription();
                    System.out.println("logos adding... " + annotation.getDescription());
                }
                
              }
              return foundLogo;
            }
          }
      
      public static LinkedList<String> matchLogoToImage(String filePath, PrintStream out, ByteString imgBytes, List<String> logos) throws Exception, IOException {
           List<AnnotateImageRequest> requests = new ArrayList<>();
           LinkedList<String> matched = new LinkedList<>();
           
           //ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
           //ByteString imgBytes = ByteString.readFrom(img);

           try{
                        
               //the next few lines create a byte array from the BufferedImage
               //you can comment these out and have this function take in a byte array directly
               //not sure if this works with a BufferedImage - I ran it on the video and it
               //didn't look like it was detecting anything but i didn't get an error back from the API
                //ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //ImageIO.write( img, "jpg", baos );
                //baos.flush();
                //byte[] imageInByte = baos.toByteArray();
                //.close();
                
                
                //below is where you would input the byte array (this turns the array into ByteString
                //which is needed for this code to work
                 // ByteString newImage = ByteString.copyFrom(imageInByte);
                   Image img2 = Image.newBuilder().setContent(imgBytes).build();

                    Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
                    AnnotateImageRequest request =
                        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img2).build();
                    requests.add(request);

                    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                      List<AnnotateImageResponse> responses = response.getResponsesList();

                      for (AnnotateImageResponse res : responses) {
                        if (res.hasError()) {
                          out.printf("Error: %s\n", res.getError().getMessage());
                          return new LinkedList<>();
                        }

                        // For full list of available annotations, see http://g.co/cloud/vision/docs
                        for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
                            String description = annotation.getDescription();
                            System.out.println("img detected... " + annotation.getDescription());
                            //matched.add(description);
                            for(int i = 0; i < logos.size(); i++) {
                                String logo = logos.get(i);
                                if(description.contains(logo) || logo.contains(description)) {
                                    matched.add(logo);
                                }
                                if(description.contains("hard rock")){
                                    matched.add("hard rock live");
                                }
                                
                            }
                        }
                        
                      }
                      
                      return matched;
                    }
                        
                }catch(IOException e){
                    System.out.println(e.getMessage());
                }       
            return null;
            }   

          
      
      public static void getNewAds(File dir, List<String> matched) {
          File[] directoryListing = dir.listFiles();
          if (directoryListing != null) {
            for (File child : directoryListing) {
                for( String match : matched) {
                    if(child.getName().contains(match)) {
                        System.out.println("Found The Ad File To Insert: " + child.getName());
                    }
                }
                System.out.println("file: " + child.getName());
            }       
          } else {
              System.out.println("No Brand Images Found");
          }
      }
      
    public static List<String> run(String path)
    {
        System.out.println("current brand image path: " + path);
        try {
            List<String> logos = new ArrayList<>();  
            Files.walk( Paths.get(path))
            .filter(s -> s.toString().endsWith(".bmp"))
            .map(Path::getFileName)
            .collect(Collectors.toList()).stream().forEach(x->{
                try {
                    logos.add(detectLogos(path + x.getFileName().toString(), System.out));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });;
                return logos;
               
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
