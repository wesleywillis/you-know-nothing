package gameofthrones;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.amazonaws.util.json.JSONTokener;

public class GameOfThronesSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(GameOfThronesSpeechlet.class);

    private static final String SLOT_CHARACTER = "Character";
    private static final String SESSION_CHARACTER = "character";
    private static final String SESSION_CHAR_CODE = "char_code";

    /**
     * URL prefix to download character information from an api of ice and fire.
     */
    private static final String ENDPOINT = "http://www.anapioficeandfire.com/api/characters/";

    //API character codes

    private static final int CHAR_CODE_TYRION_LANNISTER = 1052;
    private static final int CHAR_CODE_JAIME_LANNISTER = 529;
    private static final int CHAR_CODE_PETYR_BAELISH = 823;
    private static final int CHAR_CODE_JON_SNOW = 583;
    private static final int CHAR_CODE_JORAH_MORMONT = 1560;
    private static final int CHAR_CODE_THEON_GREYJOY = 1022;
    private static final int CHAR_CODE_KHAL_DROGO = 1346;
    private static final int CHAR_CODE_DAVOS_SEAWORTH = 1319;
    private static final int CHAR_CODE_MELISANDRE = 743;
    private static final int CHAR_CODE_STANNIS_BARATHEON = 1963;
    private static final int CHAR_CODE_ROOSE_BOLTON = 933;
    private static final int CHAR_CODE_YGRITTE = 2126;

    private static final HashMap<String, Integer> CHARACTER_URL = new HashMap<String, Integer>();

    static {
        CHARACTER_URL.put("tyrion lannister", CHAR_CODE_TYRION_LANNISTER);
        CHARACTER_URL.put("jaime lannister", CHAR_CODE_JAIME_LANNISTER);
        CHARACTER_URL.put("petyr baelish", CHAR_CODE_PETYR_BAELISH);
        CHARACTER_URL.put("jon snow", CHAR_CODE_JON_SNOW);
        CHARACTER_URL.put("jorah mormont", CHAR_CODE_JORAH_MORMONT);
        CHARACTER_URL.put("theon greyjoy", CHAR_CODE_THEON_GREYJOY);
        CHARACTER_URL.put("drogo", CHAR_CODE_KHAL_DROGO);
        CHARACTER_URL.put("davos seaworth", CHAR_CODE_DAVOS_SEAWORTH);
        CHARACTER_URL.put("melisandre", CHAR_CODE_MELISANDRE);
        CHARACTER_URL.put("stannis baratheon", CHAR_CODE_STANNIS_BARATHEON);
        CHARACTER_URL.put("ygritte", CHAR_CODE_YGRITTE);
        CHARACTER_URL.put("roose bolton", CHAR_CODE_ROOSE_BOLTON);
    };


    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = intent.getName();

        if ("GetBasicCharacterInfo".equals(intentName)) {
            return handleGetBasicCharacterInfo(intent, session);
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            // Create the plain text output.
            String speechOutput =
                    "With You Know Nothing, On Show, you can get"
                            + " biographical information about Game of Thrones characters that appear in both the books and the show."
                            + " For example, you could say, who is Jon Snow"
                            + " or, tell me who Tyrion Lannister is, or you can say exit. Now, which character are you confused about?";

            String repromptText = "Which character are you confused about?";

            return newAskResponse(speechOutput, false, repromptText, false);
        } else if ("AMAZON.StopIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else if ("AMAZON.CancelIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any session cleanup logic would go here
    }

    /**
     * Function to handle the onLaunch skill behavior.
     *
     * @return SpeechletResponse object with voice/card response to return to the user
     */
    private SpeechletResponse getWelcomeResponse() {
        String speechOutput = "My Grace, which character are you confused about?";
        // If the user either does not reply to the welcome message or says something that is not
        // understood, they will be prompted again with this text.
        String repromptText =
                "With You Know Nothing, On Show, you can get"
                        + " biographical information about Game of Thrones characters that appear in both the books and the show."
                        + " For example, you could say, who is Jon Snow"
                        + " or, tell me who Tyrion Lannister is, or you can say exit. Now, which character are you confused about?";

        return newAskResponse(speechOutput, false, repromptText, false);
    }

    /**
     * ^^^^^ might need to add a method about my slots here
     * Prepares the speech to reply to the user. Obtain bio info from an API of Ice and Fire for the character specified
     * by the user, and returns that bio in both
     * speech and SimpleCard format.
     *
     * @param intent
     *            the intent object which contains the character slot
     * @param session
     *            the session object
     * @return SpeechletResponse object with voice/card response to return to the user
     */
    private SpeechletResponse handleGetBasicCharacterInfo(Intent intent, Session session) {
        //determines character user is asking about
        CharacterValues<String, String> characterObject = null;
        try {
            characterObject = getCharCodeFromIntent(intent, true);
        } catch (Exception e) {
            //invalid character
            System.out.println(e);
            String speechOutput =
                    "I'm sorry My Grace, but I can't understand you. Mayhaps you should repeat the name?";
            //String.format("Error, %s",e.getCause().getMessage());
            return newAskResponse(speechOutput, speechOutput);
        }

        return getCharacterResponse(characterObject);
    }

    /**
     * Use this method to issue the request, and
     * respond to the user with the final answer.
     */
    private SpeechletResponse getCharacterResponse(CharacterValues<String, String> charUrl) {
        return makeCharacterRequest(charUrl);
    }

    /**
     * Gets the character from the intent, or throws an error.
     */
    private CharacterValues<String, String> getCharCodeFromIntent(final Intent intent,
            final boolean assignDefault) throws Exception {
        Slot characterSlot = intent.getSlot(SLOT_CHARACTER);
        CharacterValues<String, String> characterObject = null;
        // slots can be missing, or slots can be provided but with empty value.
        // must test for both.
        if (characterSlot == null || characterSlot.getValue() == null) {
            if (!assignDefault) {
                throw new Exception("");
            } else {
                // For sample skill, default to Jon Snow.
                characterObject =
                        new CharacterValues<String, String>("jon snow", Integer.toString(CHARACTER_URL
                                .get("jon snow")));
            }
        } else {
            // lookup the character. Sample skill uses well known mapping of a few known characters to
            // character codes.
            String characterName = characterSlot.getValue();
            if (CHARACTER_URL.containsKey(characterName.toLowerCase())) {
                characterObject =
                        new CharacterValues<String, String>(characterName, Integer.toString(CHARACTER_URL
                                .get(characterName.toLowerCase())));
            } else {
                throw new Exception(characterName);
            }
        }
        return characterObject;
    }


    /**
     * Wrapper for creating the Ask response from the input strings with
     * plain text output and reprompt speeches.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
        return newAskResponse(stringOutput, false, repromptText, false);
    }

    /**
     * Wrapper for creating the Ask response from the input strings.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param isOutputSsml
     *            whether the output text is of type SSML
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @param isRepromptSsml
     *            whether the reprompt text is of type SSML
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml,
                                             String repromptText, boolean isRepromptSsml) {
        OutputSpeech outputSpeech, repromptOutputSpeech;
        if (isOutputSsml) {
            outputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
        } else {
            outputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
        }

        if (isRepromptSsml) {
            repromptOutputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) repromptOutputSpeech).setSsml(stringOutput);
        } else {
            repromptOutputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
        }

        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }

    /**
     * Uses An API of Ice and Fire, documented at https://anapioficeandfire.com. Results can be verified at:
     * http://www.anapioficeandfire.com/api/characters/[id] .
     *
     * @see <a href = "http://www.anapioficeandfire.com/api/">An API of Ice and Fire</a>
     * @throws IOException
     */
    private SpeechletResponse makeCharacterRequest(CharacterValues<String, String> charUrl) {
        String jon = "This is a demo about Jon Snow";
        String mel = "Melisandre is a Red Priestess of the Asshai culture.  She is also known by the following aliases: The Red Priestess, The Red Woman, The King's Red Shadow, and Lot Seven.  You've seen her following Stannis around Westeros.  Remember when you freaked out because that lady gave birth to a freaky shadow man?  That was Melisandre.";

        String speechOutput = mel;
    /*    String queryString =
                String.format(charUrl.apiValue);

        String speechOutput = "";

        InputStreamReader inputStream = null;
        BufferedReader bufferedReader = null;
        StringBuilder builder = new StringBuilder();
        try {
            String line;
            URL url = new URL(ENDPOINT + "583");
            inputStream = new InputStreamReader(url.openStream(), Charset.forName("US-ASCII"));
            bufferedReader = new BufferedReader(inputStream);
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            // reset builder to a blank string
            //speechOutput = String.format("Error, %s",e.getCause().getMessage());
            builder.setLength(0);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(bufferedReader);
        }

        if (builder.length() == 0) {
           speechOutput =
                    "Sorry, the API of Ice and Fire is experiencing a problem. "
                          + "Please try again later, or maybe just read the books.";
        } else {
            try {
                JSONObject apiOfIceObject = new JSONObject(new JSONTokener(builder.toString()));
                if (apiOfIceObject != null) {
                    BioValues charBioResponse = new BioValues(apiOfIceObject.getString("name"), apiOfIceObject.getString("culture"), apiOfIceObject.getString("gender"), apiOfIceObject.getString("born"));
                    speechOutput =
                            new StringBuilder()
                                    .append(charBioResponse.name)
                                    .append(", is a ")
                                    .append(charBioResponse.culture)
                                    .append(charBioResponse.gender)
                                    .append(", who was born in, ")
                                    .append(charBioResponse.born)
                                    .append(".")
                                    .toString();
                }
            } catch (JSONException e) {
                log.error("Exception occurred when building the JSON object.", e);
            }
        }

        */

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("You Know Nothing On Show");
        card.setContent(speechOutput);

        // Create the plain text output
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(speechOutput);

        return SpeechletResponse.newTellResponse(outputSpeech, card);
    }

    /**
     * Encapsulates the return values for character bio information in a single object.
     */
    private static class BioValues {
        private final String name, gender, culture, born;

        BioValues(String name, String gender, String culture, String born) {
            this.name = name;
            this.gender = gender;
            this.culture = culture;
            this.born = born;
        }
    }

    /**
     * Encapsulates the speech and api value for characterValue objects.
     *
     * @param <L>
     *            text that will be spoken to the user
     * @param <R>
     *            text that will be passed in as an input to an API
     */
    private static class CharacterValues<L, R> {
        private final L speechValue;
        private final R apiValue;

        public CharacterValues(L speechValue, R apiValue) {
            this.speechValue = speechValue;
            this.apiValue = apiValue;
        }
    }
}