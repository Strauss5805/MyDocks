/**
 * DOCKS is a framework for post-processing results of Cloud-based speech 
 * recognition systems.
 * Copyright (C) 2014 Johannes Twiefel
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact:
 * 7twiefel@informatik.uni-hamburg.de
 */

import java.io.BufferedReader;
import java.io.Console;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import edu.cmu.sphinx.result.TokenGraphDumper;
import edu.cmu.sphinx.util.NISTAlign;
import Data.Result;
import Frontend.LocalMicrophone;
import Frontend.VoiceActivityDetector;
import PostProcessor.SentencelistPostProcessor;
import PostProcessor.SphinxBasedPostProcessor;
import PostProcessor.WordlistPostProcessor;
import Recognizer.RawGoogleRecognizer;
import Recognizer.SphinxRecognizer;
import Utils.ConfigCreator;
import Utils.ExampleChooser;
import Utils.Printer;
import Utils.TestSupervisor;

/**
 * Class showing different examples of usage
 * @author 7twiefel
 * 
 */
class Example
{

    // utility to test recognizers and postprocessors on an example audio file
    private static void testFile(String filename, String sentence,
            RawGoogleRecognizer rawGoogleRecognizer,
            SentencelistPostProcessor sentencelist,
            SphinxRecognizer sphinxNGram, SphinxRecognizer sphinxSentences,
            SphinxBasedPostProcessor sphinxBasedPostProcessorBigram,
            SphinxBasedPostProcessor sphinxBasedPostProcessorUnigram,
            SphinxBasedPostProcessor sphinxBasedPostProcessorSentences,
            WordlistPostProcessor wordlist)
    {

        // clean the sentences from special chars
        sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
        sentence = sentence.replaceAll(" +", " ");
        if (!sentence.equals(""))
            if (sentence.charAt(0) == ' ') sentence = sentence.substring(1);

        Printer.printColor(Printer.ANSI_CYAN, sentence);

        // containers for results
        String hypRawGoogle = "";
        String hypSentenceList = "";
        String hypWordList = "";
        String hypSphinxPostProcessorUnigram = "";
        String hypSphinxPostProcessorBigram = "";
        String hypSphinxPostProcessorSentences = "";

        // recognize from google
        Result r = null;
        r = rawGoogleRecognizer.recognizeFromFile(filename);

        if (r != null)
        {
            // print out result
            hypRawGoogle = r.getBestResult();
            System.out.println("Raw Google: " + hypRawGoogle);

            // recognize from google result
            r = sentencelist.recognizeFromResult(r);
            if (r != null) hypSentenceList = r.getBestResult();
            System.out.println("Google+Sentencelist: " + hypSentenceList);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);

            r = wordlist.recognizeFromResult(r);
            if (r != null) hypWordList = r.getBestResult();
            System.out.println("Google+Wordlist: " + hypWordList);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);
            // recognize from google result
            r = sphinxBasedPostProcessorBigram.recognizeFromResult(r);
            if (r != null) hypSphinxPostProcessorBigram = r.getBestResult();
            System.out.println("Google+Sphinx N-Gram: "
                    + hypSphinxPostProcessorBigram);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);
            // recognize from google result
            r = sphinxBasedPostProcessorUnigram.recognizeFromResult(r);
            if (r != null) hypSphinxPostProcessorUnigram = r.getBestResult();
            System.out.println("Google+Sphinx Unigram: "
                    + hypSphinxPostProcessorUnigram);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);
            // recognize from google result
            r = sphinxBasedPostProcessorSentences.recognizeFromResult(r);
            if (r != null) hypSphinxPostProcessorSentences = r.getBestResult();
            System.out.println("Google+Sphinx Sentences: "
                    + hypSphinxPostProcessorSentences);

        }
        // recognize from file
        r = sphinxSentences.recognizeFromFile(filename);
        String result = "";
        if (r != null)
        {
            result = r.getBestResult();

        }
        System.out.println("Sphinx Sentences: " + result);

        // recognize from file
        r = sphinxNGram.recognizeFromFile(filename);
        result = "";
        if (r != null)
        {
            result = r.getBestResult();

        }
        System.out.println("Sphinx N-Gram: " + result);

    }

    // utility to play audio
    private static void playSound(String filename)
    {

        File file = new File(filename);

        try
        {

            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);

            AudioFormat format = inputStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);

            clip = (Clip) AudioSystem.getLine(info);

            clip.open(inputStream);
            clip.start();

        }
        catch (UnsupportedAudioFileException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Get a sound clip resource.
        catch (LineUnavailableException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // interrupt till enter is pressed
    private static void waitForEnter()
    {
        Console c = System.console();
        if (c != null)
        {

            c.format("\nPress ENTER to proceed.\n");
            c.readLine();
        }
    }

    // a simple example simulation input from files
    private static void exampleSimulation(String key)
    {
        // this is the config name. it is used as prefix for all configuration
        // file like wtm_experiment.sentences.txt

        //String configname = "wtm_experiment";
        String configname = "config/elpmaxe/elpmaxe";

        // initialize some recognizers
        System.out.println("Starting Raw Google");
        RawGoogleRecognizer rawGoogle = new RawGoogleRecognizer(key);

        System.out.println("Starting Google+Sentencelist");
        SentencelistPostProcessor sentencelist = new SentencelistPostProcessor(
                configname + ".sentences", 1, key);

        System.out.println("Starting Sphinx N-Gram");
        SphinxRecognizer sphinxNGram = new SphinxRecognizer(configname
                + ".ngram.xml");

        System.out.println("Starting Sphinx Sentences");
        SphinxRecognizer sphinxSentences = new SphinxRecognizer(configname
                + ".fsgsentences.xml");

        System.out.println("Starting Google+Sphinx N-Gram");
        final SphinxBasedPostProcessor sphinxPostProcessorBigram = new SphinxBasedPostProcessor(
                configname + ".pngram.xml", configname + ".words", 0, 0, 0);

        System.out.println("Starting Google+Sphinx Unigram");
        final SphinxBasedPostProcessor sphinxPostProcessorUnigram = new SphinxBasedPostProcessor(
                configname + ".punigram.xml", configname + ".words", 0, 0, 0);

        System.out.println("Starting Google+Sphinx Sentences");
        final SphinxBasedPostProcessor sphinxPostProcessorSentences = new SphinxBasedPostProcessor(
                configname + ".pgrammarsentences.xml", configname + ".words",
                0, 0, 0);

        System.out.println("Starting Google+Wordlist");
        WordlistPostProcessor wordlist = new WordlistPostProcessor(configname
                + ".words", key);

        // a testfile
        String filename = "data/back_fs_1387386033021_m1.wav";
        // the reference text
        String sentence = "there is a door in the back";
        // play sound before recognition
        playSound(filename);
        // recognize
        testFile(filename, sentence, rawGoogle, sentencelist, sphinxNGram,
                sphinxSentences, sphinxPostProcessorBigram,
                sphinxPostProcessorUnigram, sphinxPostProcessorSentences,
                wordlist);

        waitForEnter();

        filename = "data/front_fs_1387379085134_m1.wav";
        sentence = "the door is in front of you";
        playSound(filename);
        testFile(filename, sentence, rawGoogle, sentencelist, sphinxNGram,
                sphinxSentences, sphinxPostProcessorBigram,
                sphinxPostProcessorUnigram, sphinxPostProcessorSentences,
                wordlist);
        waitForEnter();
        filename = "data/home_fs_1387379071054_m1.wav";

        sentence = "the kitchen is at home";
        playSound(filename);
        testFile(filename, sentence, rawGoogle, sentencelist, sphinxNGram,
                sphinxSentences, sphinxPostProcessorBigram,
                sphinxPostProcessorUnigram, sphinxPostProcessorSentences,
                wordlist);
        waitForEnter();

        filename = "data/show_fs_1387385878857_m1.wav";
        sentence = "robot show me the pen";
        playSound(filename);
        testFile(filename, sentence, rawGoogle, sentencelist, sphinxNGram,
                sphinxSentences, sphinxPostProcessorBigram,
                sphinxPostProcessorUnigram, sphinxPostProcessorSentences,
                wordlist);
    }

    public static void exampleLive(String key)
    {

        // define config name
        String configname = "config/elpmaxe/elpmaxe";

        // load google
        System.out.println("Starting Raw Google");
        RawGoogleRecognizer rawGoogle = new RawGoogleRecognizer(key);

        // load sentencelist postprocessor
        System.out.println("Starting Google+Sentencelist");
        SentencelistPostProcessor sentencelist = new SentencelistPostProcessor(
                configname + ".sentences", 1, key);

        Result r;
        // load example chooser
        ExampleChooser ec = new ExampleChooser(configname + ".sentences");

        // load voice activity detection
        VoiceActivityDetector vac = new VoiceActivityDetector(
                new LocalMicrophone(), "LocalMicrophone");

        while (true)
        {
            // print random sentence
            ec.printRandomExample();

            // recognize from microphone
            r = rawGoogle.recognize(vac);

            String rawGoogleResult = "";
            String sentenceListResult = "";

            if (r != null)
            {
                // get google result
                rawGoogleResult = r.getBestResult();
                System.out.println("Raw Google: " + rawGoogleResult);

                // postprocess with sentencelist
                r = sentencelist.recognizeFromResult(r);

                // get result
                if (r != null) sentenceListResult = r.getBestResult();
                System.out.println("Google+Sentencelist: " + sentenceListResult);

            }

        }
    }

    public static String join(String[] strings, String delimiter)
    {
        if (strings == null || strings.length == 0) return "";
        StringBuilder builder = new StringBuilder(strings[0]);
        for (int i = 1; i < strings.length; i++)
        {
            builder.append(delimiter)
                .append(strings[i]);
        }
        return builder.toString();
    }

    public static void main(String[] args)
    {
        // this methode will create a file in the results directory with the WER of rawGoogle
        //        testGoogleWER();
        
        // this methode tests the pngramm config file with (languageWeight,wordInsertionProbability)
//        testWER(1.67f,4.12f);
        
        //creates a the configuration data and the language model from a .batch file
        createConf("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/delete/delete.batch", "delete");
//        createConf("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/heinrichLab_L2test.batch", "scripted_test2");
//        createConf("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/heinrichLab_L3test.batch", "scripted_test3");
//        createConf("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/heinrichLab_L4test.batch", "scripted_test4");
//        createConf("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/heinrichLab_L5test.batch", "scripted_test5");
//        createConf("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/heinrichLab_L6test.batch", "scripted_test6");
//        createConf("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/heinrichLab_L7test.batch", "scripted_test7");
//        createConf("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/heinrichLab_L8test.batch", "scripted_test8");
//        createConf("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/heinrichLab_L9test.batch", "scripted_test9");
//        createConf("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/heinrichLab_L10test.batch", "scripted_test10");
               


//                findLWandWIP();
        
//                createRefHyp();
        
//                testSphinxWithConstantGoogleResults("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/TIMIT/AmplifiedSourceFiles/", "minitimit.refhyp");
       
        //        testSphinx();

//        TestSupervisor.testWERall("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/","heinrichLab_L1test.batch", "config/scripted1/scripted1", "sigscripted1");
//        TestSupervisor.testWERall("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/","heinrichLab_L2test.batch", "config/scripted2/scripted2", "sigscripted2");
//        TestSupervisor.testWERall("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/","heinrichLab_L3test.batch", "config/scripted3/scripted3", "sigscripted3");
//        TestSupervisor.testWERall("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/","heinrichLab_L4test.batch", "config/scripted4/scripted4", "sigscripted4");
//        TestSupervisor.testWERall("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/","heinrichLab_L5test.batch", "config/scripted5/scripted5", "sigscripted5");
//        TestSupervisor.testWERall("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/","heinrichLab_L6test.batch", "config/scripted6/scripted6", "sigscripted6");
//        TestSupervisor.testWERall("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/","heinrichLab_L7test.batch", "config/scripted7/scripted7", "sigscripted7");
//        TestSupervisor.testWERall("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/","heinrichLab_L8test.batch", "config/scripted8/scripted8", "sigscripted8");
//        TestSupervisor.testWERall("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/","heinrichLab_L9test.batch", "config/scripted9/scripted9", "sigscripted9");
//        TestSupervisor.testWERall("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/","heinrichLab_L10test.batch", "config/scripted10/scripted10", "sigscripted10");



        //        TestSupervisor.matlabFormatPerformance();    
        if (true) return;

        // set verbose to false
        Printer.verbose = false;

        //uncomment this to create a new configuration from a batch file
        //ConfigCreator.createConfig("elpmaxe", "./batch");

        //put your Google key here
        //String key = "yourkeyhere"; 
        String key = "AIzaSyBOti4mM-6x9WDnZIjIeyEU21OpBXqWBgw";

        //starts the simulation example
        String configname = "config/elpmaxe/elpmaxe";

        // initialize some recognizers
        System.out.println("Starting Raw Google");
        RawGoogleRecognizer rawGoogle = new RawGoogleRecognizer(key);

        System.out.println("Starting Google+Sphinx N-Gram");
        final SphinxBasedPostProcessor sphinxPostProcessorTrigram = new SphinxBasedPostProcessor(
                configname + ".pngram.xml", configname + ".words", 0, 0, 0);

        System.out.println("Starting Sphinx N-Gram");
        SphinxRecognizer sphinxNGram = new SphinxRecognizer(configname
                + ".ngram.xml");

        String filename = "data/back_fs_1387386033021_m1.wav";
        // the reference text
        // play sound before recognition
        playSound(filename);
        // recognize

        Result r = null;
        r = rawGoogle.recognizeFromFile(filename);

        if (r != null)
        {
            // print out result
            String hypRawGoogle = r.getBestResult();
            System.out.println("Raw Google: " + hypRawGoogle);

            r = new Result();
            r.addResult(hypRawGoogle);
            // recognize from google result
            r = sphinxPostProcessorTrigram.recognizeFromResult(r);
            if (r != null)
            {
                String hypSphinxPostProcessorTrigram = r.getBestResult();
                System.out.println("Google+Sphinx Unigram: "
                        + hypSphinxPostProcessorTrigram);
            }
        }
        r = sphinxNGram.recognizeFromFile(filename);
        if (r != null)
        {
            // print out result
            String hypSphinx = r.getBestResult();
            System.out.println("Raw Google: " + hypSphinx);
        }

    }

    public static void testSphinx()
    {

        // set verbose to false
        Printer.verbose = false;

        String key = "AIzaSyBOti4mM-6x9WDnZIjIeyEU21OpBXqWBgw";

        //starts the simulation example
        String configname = "config/mywords/mywords";
        //        String configname = "config/elpmaxe/elpmaxe";

        // initialize some recognizers
        System.out.println("Starting Raw Google");
        RawGoogleRecognizer rawGoogle = new RawGoogleRecognizer(key);

        System.out.println("Starting Sphinx N-Gram");
        final SphinxBasedPostProcessor sphinxPostProcessorTrigram = new SphinxBasedPostProcessor(
                configname + ".pngram.xml", configname + ".words", 0, 0, 0);
        //        SphinxRecognizer sphinxNGram = new SphinxRecognizer(configname
        //                + ".ngram.xml");
        // recognize from file

        String filename = "data/169rec.wav";
        //        playSound(filename);
        Result r;
        r = rawGoogle.recognizeFromFile(filename);

        if (r != null)
        {
            r = sphinxPostProcessorTrigram.recognizeFromResult(r);
            String result = r.getBestResult();
            System.out.println("Final Result: " + result);

            //            PhonemeCreator pc = new PhonemeCreator();
            //            ArrayList<PhonemeContainer> phonemesSpeech = pc.getPhonemes(r);
            //get best result

            //            PRINTES OUT THE RESULT IN PHONEMEFORM
            //            String[] phonemes = phonemesSpeech.get(0)
            //                .getPhonemes();
            //            System.out.print(phonemes.length+" Phoneme :");
            //            for (int i = 0; i < phonemes.length; i++)
            //            {
            //                System.out.print(phonemes[i]+", ");  
            //            }
            //            System.out.println();

        }
        else
        {
            System.out.println("result = null");
        }
    }

    public static void findLWandWIP()
    {
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/TIMIT/AmplifiedSourceFiles/", "minitimit.refhyp");
//                TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/TIMIT/AmplifiedSourceFiles/", "test2timit.google.refhyp");
        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/SCRIPTED/heinrich_speech_dataset/","heinrichLab.google.refhyp","scripted");
        //        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/Semeval/semevaltask6_evaldata/", "strauss.google.refhyp");
        //        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/SPONT/", "spont.google.refhyp");  
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/TIMIT/AmplifiedSourceFiles/", "test2timit.google.refhyp");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/heinrich_speech_dataset/", "halfheinrichLab.googl.refhyp");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L1test.batch.refhyp","scripted1");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L2test.batch.refhyp","scripted2");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L3test.batch.refhyp","scripted3");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L4test.batch.refhyp","scripted4");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L5test.batch.refhyp","scripted5");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L6test.batch.refhyp","scripted6");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L7test.batch.refhyp","scripted7");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L8test.batch.refhyp","scripted8");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L9test.batch.refhyp","scripted9");
//        TestSupervisor.findBestWIPandLW("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L10test.batch.refhyp","scripted10");
    }

    public static void testWER(float lw, float wip)
    {
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/SCRIPTED/heinrich_speech_dataset/","heinrichLab.google.refhyp",wip,lw); 
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/SPONT/", "spont.google.refhyp", wip, lw); 
//                TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/SPONT/", "spontWithEmptyResults.google.refhyp", wip, lw);      
                TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/heinrich_speech_dataset/", "heinrichLab.googl.refhyp","scripted", wip, lw);
//                TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/SCRIPTED/heinrich_speech_dataset/","smallheinrichLab.google.refhyp",wip,lw);
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/TIMIT/AmplifiedSourceFiles/", "minitimit.refhyp", lw, wip);
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/TIMIT/AmplifiedSourceFiles/", "strauss.TIMIT_google.refhyp", wip, lw);
//                TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/TIMIT/AmplifiedSourceFiles/", "test2timit.google.refhyp",wip , lw);
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/Semeval/semevaltask6_evaldata/", "strauss.semeval400small.refhyp", wip, lw);
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L1test.batch.refhyp","scripted1",wip,lw); 
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L2test.batch.refhyp","scripted2",wip,lw); 
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L3test.batch.refhyp","scripted3",wip,lw); 
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L4test.batch.refhyp","scripted4",wip,lw); 
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L5test.batch.refhyp","scripted5",wip,lw); 
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L6test.batch.refhyp","scripted6",wip,lw); 
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L7test.batch.refhyp","scripted7",wip,lw); 
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L8test.batch.refhyp","scripted8",wip,lw); 
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L9test.batch.refhyp","scripted9",wip,lw); 
//        TestSupervisor.myTestWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/", "strauss.heinrichLab_L10test.batch.refhyp","scripted10",wip,lw); 

    }

    public static void createConf(String batchpath, String name)
    {
        System.out.println("Creating configuration");
        ConfigCreator.createConfig(name, batchpath);
    }

    public static void createRefHyp()
    {
        System.out.println("Creating RefHyp");
        
        String path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/";
        String batchfile = "heinrichLab_L1test.batch";
        TestSupervisor.testGoogleOOV(path, batchfile);
        
        path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/";
        batchfile = "heinrichLab_L2test.batch";
        TestSupervisor.testGoogleOOV(path, batchfile);
        
        path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/";
        batchfile = "heinrichLab_L3test.batch";
        TestSupervisor.testGoogleOOV(path, batchfile);
        
        path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/";
        batchfile = "heinrichLab_L4test.batch";
        TestSupervisor.testGoogleOOV(path, batchfile);
        
        path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/";
        batchfile = "heinrichLab_L5test.batch";
        TestSupervisor.testGoogleOOV(path, batchfile);
             
        path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/";
        batchfile = "heinrichLab_L6test.batch";
        TestSupervisor.testGoogleOOV(path, batchfile);
        
        path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/";
        batchfile = "heinrichLab_L7test.batch";
        TestSupervisor.testGoogleOOV(path, batchfile);
        
        path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/";
        batchfile = "heinrichLab_L8test.batch";
        TestSupervisor.testGoogleOOV(path, batchfile);
        
        path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/";
        batchfile = "heinrichLab_L9test.batch";
        TestSupervisor.testGoogleOOV(path, batchfile);
        
        path = "/informatik2/students/home/1strauss/BachelorArbeit/Dataset/CrossValidation/Scripted/Test/";
        batchfile = "heinrichLab_L10test.batch";
        TestSupervisor.testGoogleOOV(path, batchfile);
        
    }

    public static void testGoogleWER()
    {
        //      TestSupervisor.myTestGoogleWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/Semeval/semevaltask6_evaldata/", "strauss.google.refhyp");
        TestSupervisor.myTestGoogleWER("/informatik2/students/home/1strauss/BachelorArbeit/Dataset/heinrich_speech_dataset/", "heinrichLab.googl.refhyp");
    }

    public static void testWERall(String path, String batchfile,
            String configname, String title)
    {
        TestSupervisor.testWERall(path, batchfile, configname, title);
    }

    public static void testSphinxWithConstantGoogleResults(String path,
            String refhyp)
    {

        // set verbose to false
        Printer.verbose = false;

        //starts the simulation example
        String configname = "config/minitimit/minitimit";
        //        String configname = "config/elpmaxe/elpmaxe";
        
        final NISTAlign alignerrefshyp = new NISTAlign(true, true);
        
        System.out.println("Starting Sphinx N-Gram");
        final SphinxBasedPostProcessor sphinxPostProcessorTrigram = new SphinxBasedPostProcessor(
                configname + ".pngram.xml", configname + ".words", 0, 0, 0);
        //        SphinxRecognizer sphinxNGram = new SphinxRecognizer(configname
        //                + ".ngram.xml");
        // recognize from file

        Result r = new Result();
        String strLine=null;
        try
        {
            BufferedReader br = null;
            try
            {
                br = new BufferedReader(
                        new InputStreamReader(new DataInputStream(
                                new FileInputStream(path + refhyp))));
            }
            catch (FileNotFoundException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            strLine = br.readLine();
//
//            strLine = br.readLine();
//            strLine = br.readLine();
//            strLine = br.readLine();

            String[] strLineSplit = strLine.split(";");
            String hypGoogle = strLineSplit[0];
            String input = strLineSplit[1];

            System.out.println("Input: " + input);
            System.out.println("GoogleHyp: " + hypGoogle);
            r.addResult(hypGoogle);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        r = sphinxPostProcessorTrigram.recognizeFromResult(r);

        if (r != null)
        {
            String[] strLineSplit = strLine.split(";");
            String input = strLineSplit[1];
            String result = r.getBestResult();
            alignerrefshyp.align(input,result);
            System.out.println(alignerrefshyp.getTotalWordErrorRate());
            
            for (String hyp : r.getResultList())
            {
                System.out.println(hyp);
            }
            //            System.out.println("Final Result: " + result);

            //            PhonemeCreator pc = new PhonemeCreator();
            //            ArrayList<PhonemeContainer> phonemesSpeech = pc.getPhonemes(r);
            //get best result

            //            PRINTES OUT THE RESULT IN PHONEMEFORM
            //            String[] phonemes = phonemesSpeech.get(0)
            //                .getPhonemes();
            //            System.out.print(phonemes.length+" Phoneme :");
            //            for (int i = 0; i < phonemes.length; i++)
            //            {
            //                System.out.print(phonemes[i]+", ");  
            //            }
            //            System.out.println();

        }
        else
        {
            System.out.println("result = null");
        }
    }

}
