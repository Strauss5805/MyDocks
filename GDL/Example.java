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
package de.unihamburg.informatik.wtm.docks;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.unihamburg.informatik.wtm.docks.data.Result;
import de.unihamburg.informatik.wtm.docks.frontend.LocalMicrophone;
import de.unihamburg.informatik.wtm.docks.frontend.VoiceActivityDetector;
import de.unihamburg.informatik.wtm.docks.postprocessor.SentencelistPostProcessor;
import de.unihamburg.informatik.wtm.docks.postprocessor.SphinxBasedPostProcessor;
import de.unihamburg.informatik.wtm.docks.postprocessor.WordlistPostProcessor;
import de.unihamburg.informatik.wtm.docks.recognizer.RawGoogleRecognizer;
import de.unihamburg.informatik.wtm.docks.recognizer.SphinxRecognizer;
import de.unihamburg.informatik.wtm.docks.utils.ExampleChooser;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class showing different examples of usage
 *
 * @author 7twiefel
 */
class Example {

    private static final Logger LOG = LoggerFactory.getLogger(Example.class);

    // utility to test recognizers and postprocessors on an example audio file
    private static void testFile(String filename, String sentence,
                                 RawGoogleRecognizer rawGoogleRecognizer,
                                 SentencelistPostProcessor sentencelist,
                                 SphinxRecognizer sphinxNGram, SphinxRecognizer sphinxSentences,
                                 SphinxBasedPostProcessor sphinxBasedPostProcessorBigram,
                                 SphinxBasedPostProcessor sphinxBasedPostProcessorUnigram,
                                 SphinxBasedPostProcessor sphinxBasedPostProcessorSentences,
                                 WordlistPostProcessor wordlist) {

        // clean the sentences from special chars
        sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
        sentence = sentence.replaceAll(" +", " ");
        if (!sentence.equals("") && sentence.charAt(0) == ' ') {
            sentence = sentence.substring(1);
        }

        LOG.info(sentence);

        // containers for results
        String hypRawGoogle;
        String hypSentenceList = "";
        String hypWordList = "";
        String hypSphinxPostProcessorUnigram = "";
        String hypSphinxPostProcessorBigram = "";
        String hypSphinxPostProcessorSentences = "";

        // recognize from google
        Result r = rawGoogleRecognizer.recognizeFromFile(filename);
        if (r != null) {
            // print out result
            hypRawGoogle = r.getBestResult();
            LOG.info("Raw Google: {}", hypRawGoogle);

            // recognize from google result
            r = sentencelist.recognizeFromResult(r);
            if (r != null) {
                hypSentenceList = r.getBestResult();
            }
            LOG.info("Google+Sentencelist: {}", hypSentenceList);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);

            r = wordlist.recognizeFromResult(r);
            if (r != null) {
                hypWordList = r.getBestResult();
            }
            LOG.info("Google+Wordlist: {}", hypWordList);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);
            // recognize from google result
            r = sphinxBasedPostProcessorBigram.recognizeFromResult(r);
            if (r != null) {
                hypSphinxPostProcessorBigram = r.getBestResult();
            }
            LOG.info("Google+Sphinx N-Gram: {}", hypSphinxPostProcessorBigram);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);
            // recognize from google result
            r = sphinxBasedPostProcessorUnigram.recognizeFromResult(r);
            if (r != null) {
                hypSphinxPostProcessorUnigram = r.getBestResult();
            }
            LOG.info("Google+Sphinx Unigram: {}", hypSphinxPostProcessorUnigram);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);
            // recognize from google result
            r = sphinxBasedPostProcessorSentences.recognizeFromResult(r);
            if (r != null) {
                hypSphinxPostProcessorSentences = r.getBestResult();
            }
            LOG.info("Google+Sphinx Sentences: {}", hypSphinxPostProcessorSentences);

        }
        // recognize from file
        r = sphinxSentences.recognizeFromFile(filename);
        String result = "";
        if (r != null) {
            result = r.getBestResult();
        }
        LOG.info("Sphinx Sentences: {}", result);

        // recognize from file
        r = sphinxNGram.recognizeFromFile(filename);
        result = "";
        if (r != null) {
            result = r.getBestResult();
        }
        LOG.info("Sphinx N-Gram: {}", result);
    }

    // utility to play audio
    private static void playSound(String filename) {
        File file = new File(filename);
        try {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);

            AudioFormat format = inputStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);

            Clip clip = (Clip) AudioSystem.getLine(info);

            clip.open(inputStream);
            clip.start();

        } catch (UnsupportedAudioFileException e) {
            LOG.error("audio file {} is not supported: {}", filename, e.getMessage());
        } catch (IOException e) {
            LOG.error("opening audio file {} failed: {}", filename, e.getMessage());
        } catch (LineUnavailableException e) {
            LOG.error("requested audio line unavailable: {}", e.getMessage());
        }
    }

    // interrupt till enter is pressed
    private static void waitForEnter() {
        Console c = System.console();
        if (c != null) {
            c.format("\nPress ENTER to proceed.\n");
            c.readLine();
        }
    }

    // a simple example simulation input from files
    private static void exampleSimulation(String key) {
        // this is the config name. it is used as prefix for all configuration
        // file like wtm_experiment.sentences.txt

        //String configname = "wtm_experiment";
        String configname = "config/elpmaxe/elpmaxe";

        // initialize some recognizers
        LOG.info("Starting Raw Google");
        RawGoogleRecognizer rawGoogle = new RawGoogleRecognizer(key);

        LOG.info("Starting Google+Sentencelist");
        SentencelistPostProcessor sentencelist = new SentencelistPostProcessor(
                configname + ".sentences", 1);

        LOG.info("Starting Sphinx N-Gram");
        SphinxRecognizer sphinxNGram = new SphinxRecognizer(configname
                + ".ngram.xml");

        LOG.info("Starting Sphinx Sentences");
        SphinxRecognizer sphinxSentences = new SphinxRecognizer(configname
                + ".fsgsentences.xml");

        LOG.info("Starting Google+Sphinx N-Gram");
        final SphinxBasedPostProcessor sphinxPostProcessorBigram = new SphinxBasedPostProcessor(
                configname + ".pngram.xml", configname + ".words", 0, 0, 0);

        LOG.info("Starting Google+Sphinx Unigram");
        final SphinxBasedPostProcessor sphinxPostProcessorUnigram = new SphinxBasedPostProcessor(
                configname + ".punigram.xml", configname + ".words", 0, 0, 0);

        LOG.info("Starting Google+Sphinx Sentences");
        final SphinxBasedPostProcessor sphinxPostProcessorSentences = new SphinxBasedPostProcessor(
                configname + ".pgrammarsentences.xml", configname + ".words", 0, 0, 0);

        LOG.info("Starting Google+Wordlist");
        WordlistPostProcessor wordlist = new WordlistPostProcessor(configname + ".words");

        // a testfile
        String filename = "data/back_fs_1387386033021_m1.wav";
        // the reference text
        String sentence = "there is a door in the back";
        // play sound before recognition
        playSound(filename);
        // recognize
        testFile(filename, sentence, rawGoogle, sentencelist,
                sphinxNGram, sphinxSentences, sphinxPostProcessorBigram,
                sphinxPostProcessorUnigram, sphinxPostProcessorSentences,
                wordlist);

        waitForEnter();

        filename = "data/front_fs_1387379085134_m1.wav";
        sentence = "the door is in front of you";
        playSound(filename);
        testFile(filename, sentence, rawGoogle, sentencelist,
                sphinxNGram, sphinxSentences, sphinxPostProcessorBigram,
                sphinxPostProcessorUnigram, sphinxPostProcessorSentences,
                wordlist);
        waitForEnter();
        filename = "data/home_fs_1387379071054_m1.wav";

        sentence = "the kitchen is at home";
        playSound(filename);
        testFile(filename, sentence, rawGoogle, sentencelist,
                sphinxNGram, sphinxSentences, sphinxPostProcessorBigram,
                sphinxPostProcessorUnigram, sphinxPostProcessorSentences,
                wordlist);
        waitForEnter();

        filename = "data/show_fs_1387385878857_m1.wav";
        sentence = "robot show me the pen";
        playSound(filename);
        testFile(filename, sentence, rawGoogle, sentencelist,
                sphinxNGram, sphinxSentences, sphinxPostProcessorBigram,
                sphinxPostProcessorUnigram, sphinxPostProcessorSentences,
                wordlist);
    }

    public static void exampleLive(String key) {
        // define config name
        String configname = "config/elpmaxe/elpmaxe";

        // load google
        LOG.info("Starting Raw Google");
        RawGoogleRecognizer rawGoogle = new RawGoogleRecognizer(key);

        // load sentencelist postprocessor
        LOG.info("Starting Google+Sentencelist");
        SentencelistPostProcessor sentencelist = new SentencelistPostProcessor(
                configname + ".sentences", 1);

        Result r;
        // load example chooser
        ExampleChooser ec = new ExampleChooser(configname + ".sentences");

        // load voice activity detection
        VoiceActivityDetector vac = new VoiceActivityDetector(
                new LocalMicrophone(), "LocalMicrophone");

        while (true) {
            // print random sentence
            ec.printRandomExample();

            // recognize from microphone
            r = rawGoogle.recognize(vac);

            String rawGoogleResult;
            String sentenceListResult = "";

            if (r != null) {
                // get google result
                rawGoogleResult = r.getBestResult();
                LOG.info("Raw Google: {}", rawGoogleResult);

                // postprocess with sentencelist
                r = sentencelist.recognizeFromResult(r);

                // get result
                if (r != null) {
                    sentenceListResult = r.getBestResult();
                }
                LOG.info("Google+Sentencelist: {}", sentenceListResult);
            }
        }
    }
    // a simple example simulation input from files
    private static void exampleLiveSphinx(String key) {
        // this is the config name. it is used as prefix for all configuration
        // file like wtm_experiment.sentences.txt

        //String configname = "wtm_experiment";
        String configname = "config/elpmaxe/elpmaxe";

        // initialize some recognizers
        LOG.info("Starting Raw Google");
        RawGoogleRecognizer rawGoogle = new RawGoogleRecognizer(key);
        
        // load sentencelist postprocessor
        LOG.info("Starting Google+Sentencelist");
        SentencelistPostProcessor sentencelist = new SentencelistPostProcessor(
                configname + ".sentences", 1);

        LOG.info("Starting Google+Sphinx N-Gram");
        final SphinxBasedPostProcessor sphinxPostProcessorBigram = new SphinxBasedPostProcessor(
                configname + ".pngram.xml", configname + ".words", 0, 0, 0);

        Result r;
        Result r2;
        // load example chooser
        ExampleChooser ec = new ExampleChooser(configname + ".sentences");

        // load voice activity detection
        VoiceActivityDetector vac = new VoiceActivityDetector(
                new LocalMicrophone(), "LocalMicrophone");

        while (true) {
            // print random sentence
            ec.printRandomExample();

            // recognize from microphone
            r = rawGoogle.recognize(vac);

            String rawGoogleResult;
            String sentenceListResult = "";
            String ngramResult = "";
            if (r != null) {
                // get google result
                rawGoogleResult = r.getBestResult();
                

                // postprocess with sentencelist
                r2 = sphinxPostProcessorBigram.recognizeFromResult(r);

                // get result
                if (r2 != null) {
                    ngramResult = r2.getBestResult();
                }
                // postprocess with sentencelist
                r2 = sentencelist.recognizeFromResult(r);

                // get result
                if (r2 != null) {
                    sentenceListResult = r2.getBestResult();
                }

                System.err.println("Raw Google: "+ rawGoogleResult);
                System.err.println("Google+Sentencelist: "+ sentenceListResult);
                System.err.println("Google+SphinxBigram: "+ ngramResult);
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

            }
        }
    }

// a simple example simulation input from files
private static void exampleLiveSphinxAndroid() {
    // this is the config name. it is used as prefix for all configuration
    // file like wtm_experiment.sentences.txt

    //String configname = "wtm_experiment";
    String configname = "config/elpmaxe/elpmaxe";


    
    // load sentencelist postprocessor
    LOG.info("Starting Google+Sentencelist");
    SentencelistPostProcessor sentencelist = new SentencelistPostProcessor(
            configname + ".sentences", 1);

    LOG.info("Starting Google+Sphinx N-Gram");
    final SphinxBasedPostProcessor sphinxPostProcessorBigram = new SphinxBasedPostProcessor(
            configname + ".pngram.xml", configname + ".words", 0, 0, 0);


    Result r2;
    // load example chooser
    ExampleChooser ec = new ExampleChooser(configname + ".sentences");

    final Logger LOG = LoggerFactory.getLogger(Example.class);
    int port = 54015;

    ServerSocket listener = null;
    Socket socket = null;
    BufferedReader in = null;
    PrintWriter out = null;
    try {
        LOG.info("starting server socket on port {}", port);
        listener = new ServerSocket(port);
        
        while (true) {
            // print random sentence
            ec.printRandomExample();

            LOG.debug("waiting for socket connection");
            socket = listener.accept();
            
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String requestString = in.readLine();
            LOG.debug("request: {}", requestString);

            List<String> expectedResults = parseExpectedSentences(requestString);
            Result googleResult = parseGoogleResult(requestString);

           
            String rawGoogleResult;
            String sentenceListResult = "";
            String ngramResult = "";

            // get google result
            rawGoogleResult = googleResult.getBestResult();
            

            // postprocess with sentencelist
            r2 = sphinxPostProcessorBigram.recognizeFromResult(googleResult);

            // get result
            if (r2 != null) {
                ngramResult = r2.getBestResult();
            }
            // postprocess with sentencelist
            r2 = sentencelist.recognizeFromResult(googleResult);

            // get result
            if (r2 != null) {
                sentenceListResult = r2.getBestResult();
            }

            System.err.println("Raw Google: "+ rawGoogleResult);
            System.err.println("Google+Sentencelist: "+ sentenceListResult);
            System.err.println("Google+SphinxBigram: "+ ngramResult);
            try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


            
            String bestResult = r2.getRawResult();
            String confidence = String.format(Locale.US, "%.02f", r2.getConfidence());

            LOG.debug("returning best result: {}, with confidence: {}", bestResult, confidence);
            out.println(bestResult + "===" + confidence);
        }
    } catch (Exception e) {
        LOG.error("error: ", e.getMessage());
    } finally {
        IOUtils.closeQuietly(listener);
        IOUtils.closeQuietly(socket);
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
    }

}

private static LinkedList<String> parseExpectedSentences(String socketRequest) {
    LinkedList<String> expectedSentences = new LinkedList<String>();

    String expected = socketRequest.split("===")[0];
    for (String e : expected.split("\\|")) {
        expectedSentences.add(e);
    }
    return expectedSentences;
}

private static Result parseGoogleResult(String socketRequest) {
    Result googleResult = new Result();

    String google = socketRequest.split("===")[1];
    for (String g : google.split("\\|")) {
        googleResult.addResult(g);
    }
    return googleResult;
}
    
private static void exampleOffline(String key) {
        // this is the config name. it is used as prefix for all configuration
        // file like wtm_experiment.sentences.txt

        //String configname = "wtm_experiment";
        String configname = "config/elpmaxe/elpmaxe";

        // load sentencelist postprocessor
        LOG.info("Starting Google+Sentencelist");
        SentencelistPostProcessor sentencelist = new SentencelistPostProcessor(
                configname + ".sentences", 1);

        LOG.info("Starting Google+Sphinx N-Gram");
        final SphinxBasedPostProcessor sphinxPostProcessorBigram = new SphinxBasedPostProcessor(
                configname + ".pngram.xml", configname + ".words", 0, 0, 0);

        Result r;
        Result r2;
        // load example chooser
        ExampleChooser ec = new ExampleChooser(configname + ".sentences");


        // print random sentence
        ec.printRandomExample();

        // recognize from microphone
        r = new Result();
        r.addResult("lean the table");

        String rawGoogleResult;
        String sentenceListResult = "";
        String ngramResult = "";
        if (r != null) {
            

            // postprocess with sentencelist
            r2 = sphinxPostProcessorBigram.recognizeFromResult(r);

            // get result
            if (r2 != null) {
                ngramResult = r2.getBestResult();
            }
            for(String res: r2.getResultList())
            	System.out.println(res);
            // postprocess with sentencelist
            r2 = sentencelist.recognizeFromResult(r);

            // get result
            if (r2 != null) {
                sentenceListResult = r2.getBestResult();
            }

            
            LOG.info("Google+Sentencelist: {}", sentenceListResult);
            LOG.info("Google+SphinxBigram: {}", ngramResult);
            


        }
    }    
    
    public static void main(String[] args) {
        BasicConfigurator.configure();

        //uncomment this to create a new configuration from a batch file
        //ConfigCreator.createConfig("elpmaxe", "./batch");

        //put your Google key here
        //String key = "yourkeyhere";
        String key = "AIzaSyBOti4mM-6x9WDnZIjIeyEU21OpBXqWBgw";

        //starts the simulation example
        //exampleSimulation(key);

        // starts the live recognition example
        //exampleLiveSphinx(key);
        exampleLiveSphinxAndroid();
        //exampleOffline(key);
    }
}
