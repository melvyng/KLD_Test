/*
 * Copyright 2018 xtecuan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.iadb.kic.kicsystem.integration.surveysreports.processing.facade;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.enterprise.concurrent.ManagedExecutorService;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iadb.kic.kicsystem.integration.surveysreports.dto.GetSurveyDetails;
import org.iadb.kic.kicsystem.integration.surveysreports.dto.GetSurveyExtendedResponseDTO;
import org.iadb.kic.kicsystem.integration.surveysreports.dto.Page;
import org.iadb.kic.kicsystem.integration.surveysreports.dto.Question;
import org.iadb.kic.kicsystem.integration.surveysreports.dto.responses.bulk.Datum;
import org.iadb.kic.kicsystem.integration.surveysreports.dto.responses.bulk.GetResponsesBulk;
import org.iadb.kic.kicsystem.integration.surveysreports.jpa.entities.*;
import org.iadb.kic.kicsystem.integration.surveysreports.jpa.facade.AnswersFacade;
import org.iadb.kic.kicsystem.integration.surveysreports.jpa.facade.RowsFacade;
import org.iadb.kic.kicsystem.integration.surveysreports.jpa.facade.H2CommonFacade;
import org.iadb.kic.kicsystem.integration.surveysreports.jpa.facade.ItemOfferingDataFacade;
import org.iadb.kic.kicsystem.integration.surveysreports.jpa.facade.ParticipantsFacade;
import org.iadb.kic.kicsystem.integration.surveysreports.jpa.facade.QuestionsFacade;
import org.iadb.kic.kicsystem.integration.surveysreports.jpa.facade.ResponsesDetailsFacade;
import org.iadb.kic.kicsystem.integration.surveysreports.jpa.facade.ResponsesFacade;
import org.iadb.kic.kicsystem.integration.surveysreports.jpa.facade.SurveysFacade;
import org.iadb.knl.knlsystem.integration.sfsm.dto.DatumRecipients;
import org.iadb.knl.knlsystem.integration.sfsm.dto.GetRecipientsResponseDTO;
import org.iadb.knl.knlsystem.integration.sfsm.jpa.facade.remote.SFISMMBeanRemote;
import org.iadb.knl.knlsystem.integration.sfsm.jpa.facade.remote.SurveyMonkeyRemoteClientRemote;
import org.iadb.kic.kicsystem.integration.surveysreports.dto.Page;
import org.iadb.kic.kicsystem.integration.surveysreports.dto.Question;

//Nuevos imports solo para procesar JSON - Melvyn Gomez 09/12/2025
// imports you'll need
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

//

/**
 *
 * @author xtecuan
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ProcessingFacade {

    @Resource
    private ManagedExecutorService executor; // Usa el pool de WildFly (default)

    private static final String FAMILY = "matrix";
    private static final String SUBTYPE = "rating";
    private static final int CHUNK_SIZE = 100; // tamaño de bloque ajustable
    private static final int MAX_FUTURES = 10; // controla cuántos bloques simultáneos se procesan

    /**
     * miguelsa 14/03/2019
     */
    //============
    private static final String FAMILY_LVL2 = "single_choice";
    private static final String SUBTYPE_LVL2 = "vertical";
    //============

    private static final Logger logger = Logger.getLogger(ProcessingFacade.class.getName());

    // Thread pool for background NDJSON processing
    private final ExecutorService fileProcessorExecutor = Executors.newSingleThreadExecutor();
    // JSON mapper
    private final ObjectMapper mapper = new ObjectMapper();
    @PersistenceContext(unitName = "surveysdbPU")
    private EntityManager em;

    // ✅ Inject this same EJB through the container proxy
    @Resource
    private SessionContext context;
    @EJB(lookup = SurveyMonkeyRemoteClientRemote.LOOKUP)
    private SurveyMonkeyRemoteClientRemote surveyMonkeyClient;
    @EJB
    private SurveysFacade surveysFacade;
    @EJB(lookup = SFISMMBeanRemote.LOOKUP)
    SFISMMBeanRemote remoteUtils;
    @EJB
    QuestionsFacade questionsFacade;
    @EJB
    AnswersFacade answersFacade;
    @EJB
    RowsFacade rowsFacade;
    @EJB
    ParticipantsFacade participantsFacade;
    @EJB
    ResponsesFacade responsesFacade;
    @EJB
    ResponsesDetailsFacade rdFacade;

    public Map<String, Object> doDownloadSurveys(String nickname) {
        Map<String, Object> r = new HashMap<>();
        try {

            GetSurveyExtendedResponseDTO surveys = null;
            if (nickname == null || nickname.equals("")) {
                surveys = surveyMonkeyClient.getAllCreatedExtendedSurveys();
            } else {
                surveys = surveyMonkeyClient.getExtendedSurveyByNickname(nickname);
            }
            if (surveys != null && surveys.getData() != null && !surveys.getData().isEmpty()) {
                logger.log(Level.SEVERE, "Found: " + surveys.getData().size() + " surveys!");
                surveys.getData().stream().forEach(current -> surveysFacade.saveSurvey(current));
                logger.log(Level.SEVERE, "Processed: " + surveys.getData().size() + " surveys!");
                r.put("surveysFound", surveys.getData().size());

            } else {
                logger.log(Level.SEVERE, "No Surveys where found!!!");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the surveys from Survey Monkey: ", e);
        }
        return r;
    }

    /**
     * 25/01/2021 Miguel S
     *
     * @return
     */
    public Map<String, Object> doDownloadSurveys() {
        Map<String, Object> r = new HashMap<>();
        try {

            GetSurveyExtendedResponseDTO surveys = null;

            /**
             * 1. Obtener el total de evaluaciones 2. Si son más de 1000, hacer
             * los bloques de llamada. 2.1 Guardar los resultados a la base
             *
             */
            Integer totalEvaluaciones = surveyMonkeyClient.getNumberOfExtendedSurveys();

            if (totalEvaluaciones > 1000) {
                int page = 1;
                int numIteraciones = (totalEvaluaciones / 1000); 
                int deltaEvaluaciones = totalEvaluaciones - (1000 * numIteraciones); 

                for (int i = 1; i <= numIteraciones; i++) {
                    page = i;
                    surveys = surveyMonkeyClient.getExtendedSurveys(1000, i, null);
                    guardarEvaluaciones(surveys, r);
                }
                
                surveys = surveyMonkeyClient.getExtendedSurveys(deltaEvaluaciones, page + 1, null);
                guardarEvaluaciones(surveys, r);
            } else {
                surveys = surveyMonkeyClient.getAllCreatedExtendedSurveys();

                guardarEvaluaciones(surveys, r);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the surveys from Survey Monkey: ", e);
        }
        return r;
    }

    private Map<String, Object> guardarEvaluaciones(GetSurveyExtendedResponseDTO surveys, Map<String, Object> r) {
        if (surveys != null && surveys.getData() != null && !surveys.getData().isEmpty()) {
            logger.info("Found: " + surveys.getData().size() + " surveys!");
            surveys.getData().stream().forEach(current -> surveysFacade.saveSurvey(current));
            logger.info("Processed: " + surveys.getData().size() + " surveys!");
            r.put("surveysFound", surveys.getData().size());

        } else {
            logger.log(Level.SEVERE, "No Surveys where found!!!");
        }

        return r;
    }

    /**
     * melvyng 03/06/2023
     * Este nuevo metodo busca solo los Ids de surveys activos, no retorna toda la info de los surveys
     */
    public Map<String, Object> doDownloadQuestions(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        List<Question> questions = new ArrayList<>();
        List<Surveys> surveys = new ArrayList<>();
        List<String> surveyIds = new ArrayList<>();
        try {
            if (surveyId != null && !surveyId.equals("")) {
                Surveys survey = surveysFacade.findBySmId(surveyId);
                logger.info("Surveys to download: " + survey);
                if (survey != null && (survey.getActiveItem() != null && survey.getActiveItem().equals("Y") ||
                        (survey.getSmNickname() != null && (survey.getSmNickname().startsWith("DRP") ||
                                survey.getSmNickname().startsWith("MOOC"))))) {
                    surveyIds.add(surveyId);
                }
            } else {
                surveyIds = surveysFacade.findAllIds();
            }
            logger.info("Surveys # to get questions: " + surveyIds.size());
            surveyIds.forEach((currentSurveyId) -> {
                logger.info("Processing Survey with ID: " + currentSurveyId);
                Surveys currentSurvey = surveysFacade.findBySmId(currentSurveyId);
                boolean isDRPSurvey = currentSurvey != null &&
                        currentSurvey.getSmNickname() != null &&
                        currentSurvey.getSmNickname().startsWith("DRP");
                boolean isMOOCSurvey = currentSurvey != null &&
                        currentSurvey.getSmNickname() != null &&
                        currentSurvey.getSmNickname().startsWith("MOOC");

                GetSurveyDetails details = surveyMonkeyClient.getSurveyDetails(currentSurveyId);
                if (details.getPages() != null && !details.getPages().isEmpty()) {

                    for (Page page : details.getPages()) {
                        if (page.getQuestions() != null && !page.getQuestions().isEmpty()) {
                            for (Question question : page.getQuestions()) {
                                questionsFacade.saveQuestion(question, page, currentSurveyId);
                                if (isDRPSurvey || isMOOCSurvey) {
                                    // For DRP/MOOC surveys, save all answers and rows, regardless of family/subtype
                                    logger.info("Processing survey DRP/MOOC: " + currentSurveyId + " and choices for question: " + question.getId());
                                    if (question.getAnswers() != null) {
                                        answersFacade.saveAnswer(question.getAnswers(), question.getId());
                                        if (question.getAnswers().getRows() != null) {
                                            logger.info("Processing rows for question: " + question.getId() + ", # of rows " + question.getAnswers().getRows().size());
                                            rowsFacade.saveRow(question.getAnswers(), question.getId());
                                        } else {
                                            logger.warning("Rows are null for question: " + question.getId());
                                        }
                                    } else {
                                        logger.warning("Answers are null for question: " + question.getId());
                                    }
                                } else {
                                    // For non-DRP/MOOC surveys, only save specific family/subtype answers and rows
                                    if (question.getFamily().equals(FAMILY) && question.getSubtype().equals(SUBTYPE)) {
                                        logger.info("Processing choices for question: " + question.getId());
                                        answersFacade.saveAnswer(question.getAnswers(), question.getId());
                                        logger.info("Processing rows for question: " + question.getId() + ", # of rows " + question.getAnswers().getRows().size());
                                        rowsFacade.saveRow(question.getAnswers(), question.getId());
                                    } else if (question.getFamily().equals(FAMILY_LVL2) && question.getSubtype().equals(SUBTYPE_LVL2)) {
                                        /**
                                         * miguelsa 14/03/2019
                                         */
                                        logger.info("Processing choices for question: " + question.getId());
                                        answersFacade.saveAnswer(question.getAnswers(), question.getId());
                                    }
                                }
                                questions.add(question);
                            }
                        }
                    }

                } else {
                    logger.log(Level.SEVERE, "Error no pages found for survey with ID: " + currentSurveyId);
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the questions from Survey Monkey: ", e);
        } finally {
            r.put("questionsProcessed", questions.size());
        }
        return r;
    }

    public Map<String, Object> OlddoDownloadQuestions(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        List<Question> questions = new ArrayList<>();
        try {
            List<Surveys> surveys = new ArrayList<>();
            if (surveyId != null && !surveyId.equals("")) {
                Surveys search = surveysFacade.findBySmId(surveyId);
                if (search != null) {
                    surveys.add(search);
                }
            } else {
                surveys = surveysFacade.findAll();
            }

            surveys.forEach((survey) -> {
                logger.info("Processing survey with ID: "
                        + survey.getSmId() + " nickName: "
                        + survey.getSmNickname());
                GetSurveyDetails details = surveyMonkeyClient.getSurveyDetails(survey.getSmId());
                if (details.getPages() != null && !details.getPages().isEmpty()) {

                    for (Page page : details.getPages()) {
                        if (page.getQuestions() != null && !page.getQuestions().isEmpty()) {
                            for (Question question : page.getQuestions()) {
                                questionsFacade.saveQuestion(question, page, survey.getSmId());
                                if (question.getFamily().equals(FAMILY) && question.getSubtype().equals(SUBTYPE)) {
                                    logger.info("Processing answers for question: " + question.getId());
                                    answersFacade.saveAnswer(question.getAnswers(), question.getId());
                                } else if (question.getFamily().equals(FAMILY_LVL2) && question.getSubtype().equals(SUBTYPE_LVL2)) {
                                    /**
                                     * miguelsa 14/03/2019
                                     */
                                    logger.info("Processing answers for question: " + question.getId());
                                    answersFacade.saveAnswer(question.getAnswers(), question.getId());
                                }
                                questions.add(question);
                            }
                        }
                    }

                } else {
                    logger.log(Level.SEVERE, "Error no pages found for survey with ID: " + survey.getSmId());
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the questions from Survey Monkey: ", e);
        } finally {
            r.put("questionsProcessed", questions.size());
        }
        return r;
    }

    public Map<String, Object> doDownloadParticipants(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        List<String> surveyIds = new ArrayList<>();
        List<DatumRecipients> participants = new ArrayList<>();
        try {
            if (surveyId != null && !surveyId.equals("")) {
                surveyIds.add(surveyId);
            } else {
                surveyIds = surveysFacade.findAllIds();
            }

            surveyIds.forEach(currentSurveyId -> {
                logger.info("Processing Survey with ID: " + currentSurveyId);
                List<String> collectorIds = surveyMonkeyClient.getCollectorIdsBySurveyId(currentSurveyId);
                collectorIds.forEach(collectorId -> {
                    logger.info("Processing Collector with ID: " + collectorId);
                    GetRecipientsResponseDTO oparticipants = surveyMonkeyClient.getRecipientsByCollectorsId(collectorId);
                    if (oparticipants != null && oparticipants.getData() != null && !oparticipants.getData().isEmpty()) {
                        oparticipants.getData().forEach(datum -> {
                            participants.add(datum);
                            logger.info("Processing Participant with ID: " + datum.getId() + " E-mail: " + datum.getEmail().toLowerCase());
                            participantsFacade.saveParticipant(datum, currentSurveyId, collectorId);
                        });
                    }
                });
            });
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the participants from Survey Monkey: ", e);
        } finally {
            r.put("participants", participants.size());
        }
        return r;
    }

    /**
     * Step 1.0 download and process Surveys in MARIADB using some information
     * from H2DB
     *
     * @param nickname
     * @return
     */
    @Asynchronous
    public Future<Map<String, Object>> downloadSurveys(String nickname) {
        Map<String, Object> r = new HashMap<>();
        if (nickname == null) {
            r.putAll(doDownloadSurveys());
        } else {
            r.putAll(doDownloadSurveys(nickname));
        }
        logger.info("Finish step 1.0 downloadSurveys");
        /*remoteUtils.sendStepConfirmationEmail(
                remoteUtils
                        .prepareDataForStepConfirmation("1.0",
                                "surveyMonkey-integration-reports->doDownloadSurveys",
                                r)
        );*/
        return new AsyncResult<>(r);
    }

    /**
     * Step 2.0 download and process Questions in MARIADB
     *
     * @param surveyId
     * @return
     */
    @Asynchronous
    public Future<Map<String, Object>> downloadQuestions(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        r.putAll(doDownloadQuestions(surveyId));
        logger.info("Finish step 2.0 downloadQuestions");
        /*remoteUtils.sendStepConfirmationEmail(
                remoteUtils
                        .prepareDataForStepConfirmation("2.0",
                                "surveyMonkey-integration-reports->doDownloadQuestions",
                                r)
        );*/
        return new AsyncResult<>(r);
    }

    /**
     * Step 3.0 download and process Participants in MARIADB
     *
     * @param surveyId
     * @return
     */
    @Asynchronous
    public Future<Map<String, Object>> downloadParticipants(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        r.putAll(doDownloadParticipants(surveyId));
        logger.info("Finish step 3.0 downloadParticipants");
        /*remoteUtils.sendStepConfirmationEmail(
                remoteUtils
                        .prepareDataForStepConfirmation("3.0",
                                "surveyMonkey-integration-reports->doDownloadParticipants",
                                r)
        );*/
        return new AsyncResult<>(r);
    }

    /**
     * Step 3.1 Update IADB Participants Metadata in MARIADB
     *
     * @return
     */
    @Asynchronous
    public Future<Map<String, Object>> updateParticipantsMetadata() {
        Map<String, Object> r = new HashMap<>();
        r.putAll(doUpdateParticipantsMetadata());
        logger.info("Finish step 3.1 updateParticipantsMetadata");
        /*remoteUtils.sendStepConfirmationEmail(
                remoteUtils
                        .prepareDataForStepConfirmation("3.1",
                                "surveyMonkey-integration-reports->doUpdateParticipantsMetadata",
                                r)
        );*/
        return new AsyncResult<>(r);
    }

    private Map<String, Object> doUpdateParticipantsMetadata() {
        Map<String, Object> r = new HashMap<>();
        try {
            List<Participants> participants = participantsFacade.findAllLikeIADB();
            participants.forEach(par -> {
                logger.info("Updating participant with email: " + par.getEmail());
                participantsFacade.updateIADBParticipant(par);
            });
            r.put("participantsUpdated", participants.size());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in method: doUpdateParticipantsMetadata", e);
        }
        return r;
    }

    /**
     * miguelsa 14/03/2019
     *
     * @return
     */
    private Map<String, Object> doUpdateParticipantsMetadataBySmId(String smId) {
        Map<String, Object> r = new HashMap<>();
        try {
            List<Participants> participants = participantsFacade.findBySmId(smId);
            participants.forEach(par -> {
                logger.info("Updating participant with email: " + par.getEmail());
                participantsFacade.updateIADBParticipant(par);
            });
            r.put("participantsUpdated", participants.size());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in method: doUpdateParticipantsMetadata", e);
        }
        return r;
    }

    /**
     * Step 4.0 download and process Responses in MARIADB
     *
     * @param surveyId
     * @return
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Map<String, Object>> downloadResponses(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        r.putAll(doDownloadResponses(surveyId));
        logger.info("Finish step 4.0 downloadResponses");
        /*remoteUtils.sendStepConfirmationEmail(
                remoteUtils
                        .prepareDataForStepConfirmation("4.0",
                                "surveyMonkey-integration-reports->doDownloadResponses",
                                r)
        );*/
        return new AsyncResult<>(r);
    }

    /**
     * Step 4.1 download and process Responses for MOOC_L1 surveys in MARIADB
     *
     * @return
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Map<String, Object>> downloadResponsesMOOCL1(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        r.putAll(doDownloadResponsesMOOCL1(surveyId));
        logger.info("Finish step 4.1 downloadResponsesMOOCL1");
        return new AsyncResult<>(r);
    }

    /**
     * Step 4.2 download and process Responses for MOOC_L3 surveys in MARIADB
     *
     * @return
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Map<String, Object>> downloadResponsesMOOCL3(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        r.putAll(doDownloadResponsesMOOCL3(surveyId));
        logger.info("Finish step 4.2 downloadResponsesMOOCL3");
        return new AsyncResult<>(r);
    }

    /**
     * Step 4.3 download and process Responses for MOOC_L3 surveys in MARIADB
     *
     * @return
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Map<String, Object>> downloadResponsesMOOCL3new(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        r.putAll(doDownloadResponsesMOOCL3new(surveyId));
        logger.info("Finish step 4.3 downloadResponsesMOOCL3new");
        return new AsyncResult<>(r);
    }

    /**
     * Step 4.4 download and process Responses for MOOC_L3 surveys in MARIADB
     *
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Map<String, Object> downloadResponsesMOOCL3JSON(String surveyId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ProcessingFacade proxy = context.getBusinessObject(ProcessingFacade.class);
            result.putAll(proxy.doDownloadResponsesMOOCL3JSON(surveyId));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during JSON download", e);
        }
        return result;
    }

    //@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED) //antes así estaba
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Map<String, Object> doDownloadResponsesMOOCL3JSON(String surveyId) throws Exception {

        Map<String, Object> result = new HashMap<>();
        List<String> surveyIds = new ArrayList<>();

        if (surveyId != null && !surveyId.trim().isEmpty()) {
            Surveys survey = surveysFacade.findBySmId(surveyId);
            logger.info("Surveys to download: " + survey);
                surveyIds.add(surveyId);
        } else {
            surveyIds = surveysFacade.findMOOCL3Ids();
        }

        logger.info("Surveys to download as JSON: " + surveyIds.size());

        int totalResponses = 0;
        List<String> outputFiles = new ArrayList<>();

        for (String currentSurveyId : surveyIds) {
            logger.info("Downloading JSON for Survey ID: " + currentSurveyId);

            // 🔹 NEW — find collectors for this survey
            List<Object[]> collectors = em.createNativeQuery(
                "SELECT coll_id, status " +
                    "FROM successFactorsDB.collectors_in_surveymonkey " +
                    "WHERE survey_id = :surveyId"
            )
            .setParameter("surveyId", currentSurveyId)
            .getResultList();

            // If no collectors, skip
            if (collectors == null || collectors.isEmpty()) {
                logger.warning("No collectors found for survey: " + currentSurveyId);
                continue;
            }

            for (Object[] col : collectors) {
                logger.info("Checking Survey ID: " + currentSurveyId + " and collector ID: " + col[0]);
                String collectorId = String.valueOf(col[0]);
                String collectorStatus = String.valueOf(col[1]);

                // 🔹 NEW — skip if already frozen
                Long frozenCount = ((Number) em.createNativeQuery(
                "SELECT COUNT(1) FROM responses " +
                    "WHERE collector_id = :collId AND skip_update = 'Y'"
                )
                .setParameter("collId", collectorId)
                .getSingleResult()).longValue();

                if (frozenCount != null && frozenCount > 0) {
                    logger.info("Skipping frozen collector: " + collectorId);
                    continue;
                }

                // 🔹 NEW — delete old details for this collector
                em.createNativeQuery(
                  "DELETE FROM responses_details " +
                        "WHERE sm_id = :smId " +
                        "AND response_id IN ( " +
                        "    SELECT response_id FROM responses " +
                        "    WHERE collector_id = :collId" +
                    ")"
                )
                .setParameter("smId", currentSurveyId)
                .setParameter("collId", collectorId)
                .executeUpdate();

                // 🔹 NEW — delete old responses for this collector
                em.createNativeQuery(
                    "DELETE FROM responses WHERE collector_id = :collId"
                )
                .setParameter("collId", collectorId)
                .executeUpdate();

                // File output
                //String filePath = "/opt/jboss/wildfly/standalone/log/survey_" + currentSurveyId + "_responses.ndjson";
                String filePath = "/opt/jboss/wildfly/standalone/log/survey_" + currentSurveyId + "_collector_" + collectorId + "_responses.ndjson";
                outputFiles.add(filePath);
                File outFile = new File(filePath);
                BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, false));

                try {
                    int page = 1;
                    boolean hasMore = true;

                    // Prefetch list
                    List<Future<GetResponsesBulk>> apiFutures = new ArrayList<>();

                    // Prefetch the first page
                    fetchNextPageJSON(apiFutures, currentSurveyId, page++);

                    ObjectMapper mapper = new ObjectMapper();

                    while (hasMore) {
                        // Wait for page result
                        GetResponsesBulk responseBulk = apiFutures.remove(0).get();

                        if (responseBulk != null &&
                                responseBulk.getData() != null &&
                                !responseBulk.getData().isEmpty()) {

                            List<Datum> responses = responseBulk.getData();
                            totalResponses += responses.size();

                            logger.info("Fetched page " + (page - 1)
                                    + " with " + responses.size() + " items");

                            // Write each Datum object as NDJSON
                            for (Datum datum : responses) {
                                String json = mapper.writeValueAsString(datum);
                                writer.write(json);
                                writer.newLine();
                            }

                            // Prefetch next page
                            if (responseBulk.getPerPage() * (page - 1) < responseBulk.getTotal()) {
                                fetchNextPage(apiFutures, currentSurveyId, page++);
                            } else {
                                hasMore = false;
                            }
                        } else {
                            hasMore = false;
                        }
                    }

                } finally {
                    writer.flush();
                    writer.close();
                }

                logger.info("Saved NDJSON file: " + outFile.getAbsolutePath());
                // start processing file asynchronously so the endpoint doesn't block
                /*executor.submit(() -> {
                    try {
                        // call a NON-TRANSACTIONAL public method inside EJB
                        ProcessingFacade proxy = context.getBusinessObject(ProcessingFacade.class);
                        proxy.asyncProcessResponses(filePath);
                    } catch (Exception e) {
                        logger.severe("Error processing NDJSON: " + e.getMessage());
                    }
                });*/
                logger.info("START processing responses MOOC L3 survey " + currentSurveyId);
                processResponsesFromFileSync(filePath); // Sequential
                logger.info("END processing responses MOOC L3 survey " + currentSurveyId);

                // 🔹 NEW — mark skip_update if collector is closed
                if ("closed".equalsIgnoreCase(collectorStatus)) {
                    em.createNativeQuery(
                                    "UPDATE responses SET skip_update = 'Y' WHERE collector_id = :collId"
                            )
                            .setParameter("collId", collectorId)
                            .executeUpdate();

                    logger.info("Collector " + collectorId + " closed → frozen");
                }
            }
        }
        result.put("files", outputFiles);
        result.put("responses", totalResponses);

        return result;
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void asyncProcessResponses(String filePath) {
        try {
            processResponsesFromFileSync(filePath);  // This one still uses REQUIRED
        } catch (Exception e) {
            logger.severe("Error in async processing: " + e.getMessage());
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseJson {
        public String id;

        @JsonProperty("survey_id")
        public String surveyId;

        @JsonProperty("recipient_id")
        public String recipientId;

        @JsonProperty("collector_id")
        public String collectorId;

        @JsonProperty("response_status")
        public String responseStatus;

        @JsonProperty("language")
        public String language;

        @JsonProperty("ip_address")
        public String ipAddress;

        @JsonProperty("total_time")
        public Integer totalTime;

        public String href;

        @JsonProperty("analyze_url")
        public String analyzeUrl;

        @JsonProperty("edit_url")
        public String editUrl;

        @JsonProperty("date_created")
        public String dateCreated;

        @JsonProperty("date_modified")
        public String dateModified;

        public List<PageJson> pages;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageJson {
        public String id;
        public List<QuestionJson> questions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionJson {
        public String id;
        public List<AnswerJson> answers;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnswerJson {
        @JsonProperty("choice_id")
        public String choiceId;

        @JsonProperty("row_id")
        public String rowId;

        public String text;

        @JsonProperty("is_correct")
        public Boolean isCorrect;

        public Integer score;
    }

    // Batch sizes, can tune if needed
    private static final int RESPONSES_BATCH = 250;
    private static final int DETAILS_BATCH = 1000;

    private void fetchNextPageJSON(List<Future<GetResponsesBulk>> futures,
                               String surveyId, int page) {

        Future<GetResponsesBulk> future = executor.submit(() -> {
            try {
                //return surveyMonkeyService.getResponses(surveyId, page);
                return surveyMonkeyClient.getResponsesBySurveyId(surveyId, page);
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "Error calling SurveyMonkey for survey " + surveyId + " page " + page, e);
                return null;
            }
        });

        futures.add(future);
    }

    public void processResponsesFromFile(String filePath) {
        fileProcessorExecutor.submit(() -> {
            try {
                ProcessingFacade proxy = context.getBusinessObject(ProcessingFacade.class);
                proxy.asyncProcessResponses(filePath);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in asynchronous file processing: " + filePath, e);
            }
        });
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void processResponsesFromFileSync(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            logger.warning("NDJSON file not accessible: " + filePath);
            return;
        }

        logger.info("Starting import from NDJSON: " + filePath);

        long responsesProcessed = 0;
        long detailsProcessed = 0;

        final int BATCH_SIZE = 50;

        try (BufferedReader br = new BufferedReader(new FileReader(file));
             MappingIterator<ResponseJson> it = mapper.readerFor(ResponseJson.class).readValues(br)) {

            while (it.hasNext()) {

                ResponseJson json = it.next();

                // Convert & persist response
                Responses r = convertToResponses(json);
                em.merge(r);
                responsesProcessed++;

                // process pages → questions → answers
                if (json.pages != null) {
                    for (PageJson p : json.pages) {
                        if (p.questions == null) continue;

                        for (QuestionJson q : p.questions) {
                            if (q.answers == null) continue;

                            for (AnswerJson a : q.answers) {

                                ResponsesDetails d = convertToDetails(json, p, q, a);
                                em.merge(d);

                                detailsProcessed++;

                                // Batch every N records
                                if (detailsProcessed % BATCH_SIZE == 0) {
                                    em.flush();
                                    em.clear();
                                    logger.info("Committed batch; detailsProcessed=" + detailsProcessed);
                                }
                            }
                        }
                    }
                }

                // batch response commits too
                if (responsesProcessed % BATCH_SIZE == 0) {
                    em.flush();
                    em.clear();
                }
            }

            // final flush
            em.flush();
            em.clear();

            logger.info(String.format(
                    "NDJSON import completed: responses=%d, details=%d",
                    responsesProcessed, detailsProcessed
            ));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing NDJSON file: " + filePath, e);
            throw e;
        }
    }


    private Responses convertToResponses(ResponseJson d) {

        Responses r = new Responses();

        // Convert String ID to Long
        if (d.id != null) {
            r.setResponseId(d.id);
        }

        if (d.surveyId != null) {
            r.setSmId(d.surveyId);
        }

        if (d.recipientId != null) {
            r.setRcptId(d.recipientId);
        }

        if (d.collectorId != null) {
            r.setCollectorId(d.collectorId);
        }

        r.setDateCreated(parseSMDate(d.dateCreated));
        r.setDateModified(parseSMDate(d.dateModified));

        r.setIpAddress(d.ipAddress);
        r.setResponseStatus(d.responseStatus);
        r.setResponseLanguage(d.language);
        r.setTotalTime(d.totalTime);

        r.setResponseHref(d.href);
        r.setAnalyzeUrl(d.analyzeUrl);
        r.setEditUrl(d.editUrl);

        return r;
    }


    private ResponsesDetails convertToDetails(ResponseJson r, PageJson p, QuestionJson q, AnswerJson a) {
        ResponsesDetails d = new ResponsesDetails();

        d.setSmId(r.surveyId);
        d.setResponseId(r.id);
        d.setRcptId(r.recipientId);
        d.setPageId(p.id);
        d.setQuestionId(q.id);
        d.setChoiceId(a.choiceId);
        d.setRowId(a.rowId);
        d.setIsCorrect(a.isCorrect);
        d.setScore(a.score);
        d.setOpenAnswerText(a.text);

        return d;
    }

    private Date parseSMDate(String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            OffsetDateTime odt = OffsetDateTime.parse(str);
            return Date.from(odt.toInstant());
        } catch (Exception e) {
            logger.warning("Failed to parse timestamp: " + str);
            return null;
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private Map<String, Object> doDownloadResponses(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        List<String> surveyIds = new ArrayList<>();
        int totalResponses = 0;

        try {
            if (surveyId != null && !surveyId.equals("")) {
                Surveys survey = surveysFacade.findBySmId(surveyId);
                logger.info("Surveys to download: " + survey);
                if (survey != null && (survey.getActiveItem() != null && survey.getActiveItem().equals("Y") ||
                        (survey.getSmNickname() != null && survey.getSmNickname().startsWith("DRP")))) {
                    surveyIds.add(surveyId);
                }
            } else {
                surveyIds = surveysFacade.findAllIds();
            }
            logger.info("Surveys # to get responses: " + surveyIds.size());

            for (String currentSurveyId : surveyIds) {
                logger.info("Processing Survey with ID: " + currentSurveyId);
                Surveys currentSurvey = surveysFacade.findBySmId(currentSurveyId);
                boolean isDRPSurvey = currentSurvey != null &&
                        currentSurvey.getSmNickname() != null &&
                        currentSurvey.getSmNickname().startsWith("DRP");

                int page = 1;
                int surveyTotal = 0;
                boolean hasMore = true;

                // Lista de tareas activas
                List<Future<?>> futures = new ArrayList<>();

                while (hasMore) {
                    GetResponsesBulk responses = surveyMonkeyClient.getResponsesBySurveyId(currentSurveyId, page);

                    if (!responses.isNull() && responses.getData() != null && !responses.getData().isEmpty()) {
                        List<Datum> data = responses.getData();
                        surveyTotal += data.size();
                        totalResponses += data.size();
                        logger.info("Processing page " + page + " with " + data.size() + " responses");

                        // --- dividir en bloques (chunks) ---
                        List<List<Datum>> chunks = chunkList(data, CHUNK_SIZE);

                        for (List<Datum> chunk : chunks) {
                            Future<?> f = executor.submit(() -> {
                                for (Datum datum : chunk) {
                                    try {
                                        processResponse(datum, currentSurveyId, isDRPSurvey);
                                    } catch (Exception e) {
                                        logger.log(Level.SEVERE, "Error processing response: " + datum.getId(), e);
                                    }
                                }
                            });
                            futures.add(f);

                            // Espera a que terminen si hay demasiadas tareas pendientes
                            if (futures.size() >= MAX_FUTURES) {
                                waitForFutures(futures);
                            }
                        }

                        // Check if there are more pages to fetch
                        if (responses.getPerPage() * page >= responses.getTotal()) {
                            hasMore = false;
                        } else {
                            page++;
                        }
                    } else {
                        hasMore = false;
                    }
                }
                // Esperar a que terminen las tareas restantes
                waitForFutures(futures);
                logger.info("Processed " + surveyTotal + " responses for survey " + currentSurveyId);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the responses from Survey Monkey: ", e);
        } finally {
            r.put("responses", totalResponses);
        }
        return r;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private Map<String, Object> OlddoDownloadResponses(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        List<String> surveyIds = new ArrayList<>();
        int totalResponses = 0;
        try {
            if (surveyId != null && !surveyId.equals("")) {
                Surveys survey = surveysFacade.findBySmId(surveyId);
                logger.info("Surveys to download: " + survey);
                if (survey != null && (survey.getActiveItem() != null && survey.getActiveItem().equals("Y") ||
                        (survey.getSmNickname() != null && survey.getSmNickname().startsWith("DRP")))) {
                    surveyIds.add(surveyId);
                }
            } else {
                surveyIds = surveysFacade.findAllIds();
            }
            logger.info("Surveys # to get responses: " + surveyIds.size());

            for (String currentSurveyId : surveyIds) {
                logger.info("Processing Survey with ID: " + currentSurveyId);
                Surveys currentSurvey = surveysFacade.findBySmId(currentSurveyId);
                boolean isDRPSurvey = currentSurvey != null &&
                        currentSurvey.getSmNickname() != null &&
                        currentSurvey.getSmNickname().startsWith("DRP");

                int page = 1;
                int surveyTotal = 0;
                boolean hasMore = true;

                while (hasMore) {
                    GetResponsesBulk responses = surveyMonkeyClient.getResponsesBySurveyId(currentSurveyId, page);

                    if (!responses.isNull() && responses.getData() != null && !responses.getData().isEmpty()) {
                        surveyTotal += responses.getData().size();
                        logger.info("Processing page " + page + " with " + responses.getData().size() + " responses");

                        // --- Process each response individually in a new transaction ---
                        for (Datum datum : responses.getData()) {
                            try {
                                processResponse(datum, currentSurveyId, isDRPSurvey);
                                totalResponses++;
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Error processing response: " + datum.getId(), e);
                                // Continue with next response
                            }
                        }

                        // Check if there are more pages to fetch
                        if (responses.getPerPage() * page >= responses.getTotal()) {
                            hasMore = false;
                        } else {
                            page++;
                        }
                    } else {
                        hasMore = false;
                    }
                }
                logger.info("Processed " + surveyTotal + " responses for survey " + currentSurveyId);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the responses from Survey Monkey: ", e);
        } finally {
            r.put("responses", totalResponses);
        }
        return r;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void processResponse(Datum datum, String currentSurveyId, boolean isDRPSurvey) {
        logger.info("Processing response with ID: " + datum.getId() + " for surveyId: " + currentSurveyId);
        responsesFacade.saveResponse(datum);

        if (datum.getMetadata() != null) {
            participantsFacade.updateParticipantMetadata(currentSurveyId,
                    datum.getCollectorId(),
                    datum.getRecipientId(),
                    datum.getMetadata());
        }

        if (datum.getPages() != null && !datum.getPages().isEmpty()) {
            datum.getPages().forEach(pageData -> {
                if (pageData.getQuestions() != null && !pageData.getQuestions().isEmpty()) {
                    pageData.getQuestions().forEach(question -> {
                        Questions q = questionsFacade.findByQuestionId(question.getId());
                        if (q != null && q.getQid() > 0) {
                            if (isDRPSurvey) {
                                // For DRP surveys, save all responses regardless of family/subtype
                                logger.info("Processing survey DRP: " + currentSurveyId + " and question Id: " + q.getQid());
                                rdFacade.saveResponseDetail(question, pageData.getId(), datum.getRecipientId(), currentSurveyId, datum.getId());
                            } else {
                                // For non-DRP surveys, only save specific family/subtype responses
                                if (q.getFamily().equals(FAMILY) && q.getSubtype().equals(SUBTYPE)) {
                                    rdFacade.saveResponseDetail(question, pageData.getId(), datum.getRecipientId(), currentSurveyId, datum.getId());
                                } else if (q.getFamily().equals(FAMILY_LVL2) && q.getSubtype().equals(SUBTYPE_LVL2)) {
                                    rdFacade.saveResponseDetailLevel2(question, pageData.getId(), datum.getRecipientId(), currentSurveyId);
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private Map<String, Object> doDownloadResponsesMOOCL1(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        List<String> surveyIds = new ArrayList<>();
        int totalResponses = 0;

        try {
            surveyIds = surveysFacade.findMOOCL1Ids();
            logger.info("Surveys # to get MOOC_L1 responses: " + surveyIds.size());

            for (String currentSurveyId : surveyIds) {
                logger.info("Processing Survey with ID: " + currentSurveyId);

                int page = 1;
                int surveyTotal = 0;
                boolean hasMore = true;

                // Lista de tareas activas
                List<Future<?>> futures = new ArrayList<>();

                while (hasMore) {
                    GetResponsesBulk responses = surveyMonkeyClient.getResponsesBySurveyId(currentSurveyId, page);

                    if (!responses.isNull() && responses.getData() != null && !responses.getData().isEmpty()) {
                        List<Datum> data = responses.getData();
                        surveyTotal += data.size();
                        totalResponses += data.size();
                        logger.info("Processing page " + page + " with " + data.size() + " responses");

                        // --- dividir en bloques (chunks) ---
                        List<List<Datum>> chunks = chunkList(data, CHUNK_SIZE);

                        for (List<Datum> chunk : chunks) {
                            Future<?> f = executor.submit(() -> {
                                for (Datum datum : chunk) {
                                    try {
                                        processResponseMOOC(datum, currentSurveyId);
                                    } catch (Exception e) {
                                        logger.log(Level.SEVERE, "Error processing response: " + datum.getId(), e);
                                    }
                                }
                            });
                            futures.add(f);

                            // Espera a que terminen si hay demasiadas tareas pendientes
                            if (futures.size() >= MAX_FUTURES) {
                                waitForFutures(futures);
                            }
                        }

                        // Check if there are more pages to fetch
                        if (responses.getPerPage() * page >= responses.getTotal()) {
                            hasMore = false;
                        } else {
                            page++;
                        }

                    } else {
                        hasMore = false;
                    }
                }

                // Esperar a que terminen las tareas restantes
                waitForFutures(futures);
                logger.info("Processed " + surveyTotal + " responses for survey " + currentSurveyId);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the MOOC_L1 responses from Survey Monkey: ", e);
        } finally {
            r.put("responses", totalResponses);
        }
        return r;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private Map<String, Object> OlddoDownloadResponsesMOOCL1(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        List<String> surveyIds = new ArrayList<>();
        int totalResponses = 0;
        try {
            surveyIds = surveysFacade.findMOOCL1Ids();
            logger.info("Surveys # to get MOOC_L1 responses: " + surveyIds.size());

            for (String currentSurveyId : surveyIds) {
                logger.info("Processing Survey with ID: " + currentSurveyId);

                int page = 1;
                int surveyTotal = 0;
                boolean hasMore = true;

                while (hasMore) {
                    GetResponsesBulk responses = surveyMonkeyClient.getResponsesBySurveyId(currentSurveyId, page);

                    if (!responses.isNull() && responses.getData() != null && !responses.getData().isEmpty()) {
                        surveyTotal += responses.getData().size();
                        logger.info("Processing page " + page + " with " + responses.getData().size() + " responses");

                        // Process each response individually
                        for (Datum datum : responses.getData()) {
                            try {
                                processResponseMOOC(datum, currentSurveyId);
                                totalResponses++;
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Error processing response: " + datum.getId(), e);
                                // Continue with next response
                            }
                        }

                        // Check if there are more pages to fetch
                        if (responses.getPerPage() * page >= responses.getTotal()) {
                            hasMore = false;
                        } else {
                            page++;
                        }
                    } else {
                        hasMore = false;
                    }
                }
                logger.info("Processed " + surveyTotal + " responses for survey " + currentSurveyId);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the MOOC_L1 responses from Survey Monkey: ", e);
        } finally {
            r.put("responses", totalResponses);
        }
        return r;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private Map<String, Object> doDownloadResponsesMOOCL3(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        List<String> surveyIds = new ArrayList<>();
        int totalResponses = 0;

        try {
            surveyIds = surveysFacade.findMOOCL3Ids();
            logger.info("Surveys # to get MOOC_L3 responses: " + surveyIds.size());

            for (String currentSurveyId : surveyIds) {
                logger.info("Processing Survey with ID: " + currentSurveyId);

                int page = 1;
                int surveyTotal = 0;
                boolean hasMore = true;

                // Lista de tareas activas
                List<Future<?>> futures = new ArrayList<>();

                while (hasMore) {
                    GetResponsesBulk responses = surveyMonkeyClient.getResponsesBySurveyId(currentSurveyId, page);

                    if (!responses.isNull() && responses.getData() != null && !responses.getData().isEmpty()) {
                        List<Datum> data = responses.getData();
                        surveyTotal += data.size();
                        totalResponses += data.size();
                        logger.info("Processing page " + page + " with " + data.size() + " responses");

                        // --- dividir en bloques (chunks) ---
                        List<List<Datum>> chunks = chunkList(data, CHUNK_SIZE);

                        for (List<Datum> chunk : chunks) {
                            Future<?> f = executor.submit(() -> {
                                for (Datum datum : chunk) {
                                    try {
                                        processResponseMOOC(datum, currentSurveyId);
                                    } catch (Exception e) {
                                        logger.log(Level.SEVERE, "Error processing response: " + datum.getId(), e);
                                    }
                                }
                            });
                            futures.add(f);

                            // Espera a que terminen si hay demasiadas tareas pendientes
                            if (futures.size() >= MAX_FUTURES) {
                                waitForFutures(futures);
                            }
                        }

                        // Check if there are more pages to fetch
                        if (responses.getPerPage() * page >= responses.getTotal()) {
                            hasMore = false;
                        } else {
                            page++;
                        }

                    } else {
                        hasMore = false;
                    }
                }

                // Esperar a que terminen las tareas restantes
                waitForFutures(futures);
                logger.info("Processed " + surveyTotal + " responses for survey " + currentSurveyId);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the MOOC_L3 responses from Survey Monkey: ", e);
        } finally {
            r.put("responses", totalResponses);
        }
        return r;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private Map<String, Object> doDownloadResponsesMOOCL3new(String surveyId) {
        Map<String, Object> result = new HashMap<>();
        List<String> surveyIds = new ArrayList<>();
        int totalResponses = 0;

        try {
            // Fetch MOOC L3 survey IDs
            surveyIds = surveysFacade.findMOOCL3Ids();
            logger.info("Surveys # to get MOOC_L3 responses: " + surveyIds.size());

            for (String currentSurveyId : surveyIds) {
                logger.info("Processing Survey with ID: " + currentSurveyId);

                int page = 1;
                int surveyTotal = 0;
                boolean hasMore = true;

                // Active task list for threads handling API calls
                List<Future<GetResponsesBulk>> apiFutures = new ArrayList<>();
                // Accumulation buffer for responses
                List<Datum> accumulatedResponses = new ArrayList<>();

                // Prefetch initial API requests to reduce latency
                fetchNextPage(apiFutures, currentSurveyId, page++);

                while (hasMore) {
                    // Wait for the next API call to complete and fetch results
                    GetResponsesBulk responses = apiFutures.remove(0).get();

                    if (!responses.isNull() && responses.getData() != null && !responses.getData().isEmpty()) {
                        List<Datum> data = responses.getData();
                        surveyTotal += data.size();
                        totalResponses += data.size();
                        logger.info("Processing page " + (page - 1) + " with " + data.size() + " responses");

                        // Add responses to accumulation list
                        accumulatedResponses.addAll(data);

                        // Initiate a new prefetch for the next page
                        if (responses.getPerPage() * (page - 1) < responses.getTotal()) {
                            fetchNextPage(apiFutures, currentSurveyId, page++);
                        } else {
                            hasMore = false;
                        }

                        // Process accumulated responses when reaching 1000
                        if (accumulatedResponses.size() >= 1000) {
                            processBatch(accumulatedResponses, currentSurveyId);
                        }
                    } else {
                        hasMore = false;
                    }
                }

                // Handle leftover accumulated responses (< 1000)
                if (!accumulatedResponses.isEmpty()) {
                    processBatch(accumulatedResponses, currentSurveyId);
                }

                logger.info("Processed " + surveyTotal + " responses for survey " + currentSurveyId);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the MOOC_L3 responses from Survey Monkey: ", e);
        } finally {
            result.put("responses", totalResponses);
        }
        return result;
    }

    /**
     * Process a batch of responses in parallel.
     */
    private void processBatch(List<Datum> accumulatedResponses, String currentSurveyId) throws Exception {
        List<Datum> batchToProcess = new ArrayList<>(accumulatedResponses);
        accumulatedResponses.clear();

        Future<?> futureTask = executor.submit(() -> {
            try {
                for (Datum datum : batchToProcess) {
                    // Optimized helper method
                    processResponseMOOCnew(datum, currentSurveyId);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error processing batch: ", e);
            }
        });
        waitForFutures(Collections.singletonList(futureTask));
    }

    /**
     * Submit a prefetch request for the next page of API responses.
     */
    private void fetchNextPage(List<Future<GetResponsesBulk>> apiFutures, String surveyId, int page) {
        Future<GetResponsesBulk> apiCall = executor.submit(() -> {
            return surveyMonkeyClient.getResponsesBySurveyId(surveyId, page);
        });
        apiFutures.add(apiCall);
    }

    /** Divide una lista en sublistas del tamaño indicado */
    private <T> List<List<T>> chunkList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(list.size(), i + chunkSize)));
        }
        return chunks;
    }

    /** Espera que terminen las tareas y limpia la lista */
    private void waitForFutures(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error waiting for async task completion", e);
            }
        }
        futures.clear();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private Map<String, Object> OlddoDownloadResponsesMOOCL3(String surveyId) {
        Map<String, Object> r = new HashMap<>();
        List<String> surveyIds = new ArrayList<>();
        int totalResponses = 0;
        try {
            surveyIds = surveysFacade.findMOOCL3Ids();
            logger.info("Surveys # to get MOOC_L3 responses: " + surveyIds.size());

            for (String currentSurveyId : surveyIds) {
                logger.info("Processing Survey with ID: " + currentSurveyId);

                int page = 1;
                int surveyTotal = 0;
                boolean hasMore = true;

                while (hasMore) {
                    GetResponsesBulk responses = surveyMonkeyClient.getResponsesBySurveyId(currentSurveyId, page);

                    if (!responses.isNull() && responses.getData() != null && !responses.getData().isEmpty()) {
                        surveyTotal += responses.getData().size();
                        logger.info("Processing page " + page + " with " + responses.getData().size() + " responses");

                        // Process each response individually
                        for (Datum datum : responses.getData()) {
                            try {
                                processResponseMOOC(datum, currentSurveyId);
                                totalResponses++;
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Error processing response: " + datum.getId(), e);
                                // Continue with next response
                            }
                        }

                        // Check if there are more pages to fetch
                        if (responses.getPerPage() * page >= responses.getTotal()) {
                            hasMore = false;
                        } else {
                            page++;
                        }
                    } else {
                        hasMore = false;
                    }
                }
                logger.info("Processed " + surveyTotal + " responses for survey " + currentSurveyId);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading the MOOC_L3 responses from Survey Monkey: ", e);
        } finally {
            r.put("responses", totalResponses);
        }
        return r;
    }

    // Helper method for MOOC surveys (L1 and L3)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void processResponseMOOC(Datum datum, String currentSurveyId) {
        logger.info("Processing response with ID: " + datum.getId() + " for surveyId: " + currentSurveyId);
        responsesFacade.saveResponse(datum);

        if (datum.getMetadata() != null) {
            participantsFacade.updateParticipantMetadata(currentSurveyId,
                    datum.getCollectorId(),
                    datum.getRecipientId(),
                    datum.getMetadata());
        }

        if (datum.getPages() != null && !datum.getPages().isEmpty()) {
            datum.getPages().forEach(pageData -> {
                if (pageData.getQuestions() != null && !pageData.getQuestions().isEmpty()) {
                    pageData.getQuestions().forEach(question -> {
                        Questions q = questionsFacade.findByQuestionId(question.getId());
                        if (q != null && q.getQid() > 0) {
                            // For MOOC surveys, save all responses regardless of family/subtype
                            logger.info("Processing survey MOOC: " + currentSurveyId + " and question Id: " + q.getQid());
                            rdFacade.saveResponseDetail(question, pageData.getId(), datum.getRecipientId(), currentSurveyId, datum.getId());
                        }
                    });
                }
            });
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void processResponseMOOCnew(Datum datum, String currentSurveyId) {
        logger.info("Processing response with ID: " + datum.getId() + " for surveyId: " + currentSurveyId);

        // Step 1: Save response details
        responsesFacade.saveResponse(datum);

        // Step 2: Update participant metadata, if available
        if (datum.getMetadata() != null) {
            participantsFacade.updateParticipantMetadata(
                    currentSurveyId,
                    datum.getCollectorId(),
                    datum.getRecipientId(),
                    datum.getMetadata()
            );
        }

        // Step 3: Process pages inside the response
        if (datum.getPages() != null && !datum.getPages().isEmpty()) {
            datum.getPages().forEach(pageData -> {
                if (pageData.getQuestions() != null && !pageData.getQuestions().isEmpty()) {
                    pageData.getQuestions().forEach(question -> {
                        Questions q = questionsFacade.findByQuestionId(question.getId());

                        if (q != null && q.getQid() > 0) {
                            logger.info("Processing survey MOOC: " + currentSurveyId + " and question ID: " + question.getId());

                            // Save the response detail using the new method
                            rdFacade.saveResponseDetailnew(question, pageData.getId(), datum.getRecipientId(),
                                    currentSurveyId, datum.getId());
                        }
                    });
                }
            });
        }
    }

    /**
     * Step 5.0 make Success Factors Historic Data H2
     *
     * @return
     */
    @Asynchronous
    public Future<Map<String, Object>> makeSuccessFactorsHistoricData() {
        Map<String, Object> r = new HashMap<>();
        r.putAll(doMakeSuccessFactorsHistoricData());
        logger.info("Finish step_2_6_makeSuccessFactorsHistoricData");
        /*remoteUtils.sendStepConfirmationEmail(
                remoteUtils
                        .prepareDataForStepConfirmation("2.6",
                                "surveyMonkey-integration-reports->makeSuccessFactorsHistoricData",
                                r)
        );*/
        return new AsyncResult<>(r);
    }

    @EJB
    private ItemOfferingDataFacade itemOfferingDataFacade;

    private Map<String, Object> doMakeSuccessFactorsHistoricData() {
        Map<String, Object> r = new HashMap<>();
        r.put("insertsHistoricData", itemOfferingDataFacade.insertBasicDataFromHist());
        r.put("updateHistoricData", itemOfferingDataFacade.updateCustomValues());
        return r;
    }

    /**
     * miguelsa 13/03/2019
     *
     * @param nickname
     * @return
     */
    @Asynchronous
    public Future<Map<String, Object>> downloadSurveysByNickName(String nickname) {
        Map<String, Object> r = new HashMap<>();
        String ids = surveyMonkeyClient.getJsonByNickName(nickname);

        for (String idSurvey : ids.split(",")) {
            r.putAll(doDownloadQuestions(idSurvey));
            r.putAll(doDownloadParticipants(idSurvey));
            //r.putAll(doUpdateParticipantsMetadataBySmId(idSurvey)); //trabajarlo por idSurvey
            r.putAll(doDownloadResponses(idSurvey));
        }
        logger.info("Finish step_downloadSurveysByNickName");
        /*remoteUtils.sendStepConfirmationEmail(
                remoteUtils
                        .prepareDataForStepConfirmation("1.0",
                                "surveyMonkey-integration-reports->doDownloadSurveys :: Report level 2",
                                r)
        );*/
        return new AsyncResult<>(r);
    }
}
