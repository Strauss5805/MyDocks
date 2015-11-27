import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

import Data.Result;
import PostProcessor.SphinxBasedPostProcessor;
import Utils.Printer;


public class JUnitTest
{

    @Test
    public void test()
    {
        Printer.verbose = false;
        String path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/heinrich_speech_dataset/";
        String refhyp = "heinrichLab.google.refhyp";
        //starts the simulation example
        String configname = "config/mywords/mywords";
        //        String configname = "config/elpmaxe/elpmaxe";
    
//        System.out.println("Starting Sphinx N-Gram");
        final SphinxBasedPostProcessor sphinxPostProcessorTrigram = new SphinxBasedPostProcessor(
                configname + ".pngram.xml", configname + ".words", 0, 0, 0);
        //        SphinxRecognizer sphinxNGram = new SphinxRecognizer(configname
        //                + ".ngram.xml");
        // recognize from file
        
        Result r=new Result();
        String strLine;
        try
        {
            BufferedReader br = null;
            try
            {
                br = new BufferedReader(new InputStreamReader(
                        new DataInputStream(new FileInputStream(path
                                + refhyp))));
            }
            catch (FileNotFoundException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            strLine = br.readLine();
                String[] strLineSplit = strLine.split(";");
                String hypGoogle = strLineSplit[0];
                String input = strLineSplit[1];
                System.out.println("Input: "+ input);
                System.out.println("GoogleHyp: "+ hypGoogle);
                r.addResult(hypGoogle);
                r = sphinxPostProcessorTrigram.recognizeFromResult(r);


        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    
        r = sphinxPostProcessorTrigram.recognizeFromResult(r);
        
        if (r != null)
        {
            String result = r.getBestResult();
            System.out.println("Final Result: " + result);
    
        }
        else
        {
            fail("result = null;");
        }
    }

}
