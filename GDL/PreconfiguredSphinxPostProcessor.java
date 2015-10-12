/**
 * DOCKS is a framework for post-processing results of Cloud-based speech
 * recognition systems.
 * Copyright (C) 2014 Johannes Twiefel
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * Contact:
 * 7twiefel@informatik.uni-hamburg.de
 */
package de.unihamburg.informatik.wtm.docks;

import de.unihamburg.informatik.wtm.docks.data.Result;
import de.unihamburg.informatik.wtm.docks.postprocessor.SentencelistPostProcessor;
import de.unihamburg.informatik.wtm.docks.postprocessor.SphinxBasedPostProcessor;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class PreconfiguredSphinxPostProcessor {

    public static final int PORT = 54015;

    private static final Logger LOG = LoggerFactory.getLogger(PreconfiguredSphinxPostProcessor.class);

    /**
     * Creates a socket server which will pass incoming requests to the
     * DOCKS sentence list post processor. A request consists of a
     * (domain specific) sentence list and the n-best list of hypotheses
     * from Google Search by Voice. List entries have to be separated
     * by the pipe character '|' and the two lists by three equal signs
     * '==='. Example request:
     * "go left|go right===go left|goal left|go to left|goal to left"
     * <p/>
     * It will then return the sentence with the least phonetic
     * Levenshtein distance to a Google hypothesis, together with
     * a confidence estimate. Example response:
     * "go left===1.0"
     *
     * @param args optional params
     *             args[0] overrides the default socket port (54015)
     */
    public static void main(String[] args) {
        int port = PORT;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                LOG.warn("invalid socket port, falling back to default ({}).", PORT);
            }
        }
        String path_to_sentencelist = "/informatik/isr/wtm/home/twiefel/demo_ws/src/dialog/scripts/demo.sentences";
        //String path_to_sentencelist = "/informatik/isr/wtm/home/twiefel/demo_ws/src/dialog/scripts/pancho.sentences";
        //String path_to_sentencelist = "/informatik/isr/wtm/home/twiefel/demo_ws/src/dialog/scripts/spanish.sentences";
        //String path_to_sentencelist = "/informatik/isr/wtm/home/twiefel/demo_ws/src/dialog/scripts/brasil.sentences";
        
        //SentencelistPostProcessor sp = new SentencelistPostProcessor(path_to_sentencelist, 1);
        String configname = "config/elpmaxe/elpmaxe";
        SentencelistPostProcessor sp = new SentencelistPostProcessor(configname + ".sentences", 1);

        SphinxBasedPostProcessor sphinxPostProcessorBigram = new SphinxBasedPostProcessor(configname + ".pngram.xml", configname + ".words", 0, 0, 0);
        ServerSocket listener = null;
        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            LOG.info("starting server socket on port {}", port);
            listener = new ServerSocket(port);
            while (true) {
                LOG.debug("waiting for socket connection");
                socket = listener.accept();
                
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String requestString = in.readLine();
                LOG.debug("request: {}", requestString);

                List<String> expectedResults = parseExpectedSentences(requestString);
                Result googleResult = parseGoogleResult(requestString);

                
                Result r = sp.recognizeFromResult(googleResult);
                
                
                String bestResult = r.getRawResult();
                String confidence = String.format(Locale.US, "%.02f", r.getConfidence());

                LOG.debug("SentenceList: returning best result: {}, with confidence: {}", bestResult, confidence);
                out.println(bestResult + "===" + confidence);
                
                r = sphinxPostProcessorBigram.recognizeFromResult(googleResult);
                bestResult = r.getRawResult();
                confidence = String.format(Locale.US, "%.02f", 1.0);

                LOG.debug("SphinxNgram: returning best result: {}, with confidence: {}", bestResult, confidence);

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

}
