package com.sixpay.partner.cucumber.steps;

import com.sixpay.partner.cucumber.PartnerScenarioState;
import com.sixpay.partner.infrastructure.audit.PartnerAuditSpringDataRepository;
import com.sixpay.partner.infrastructure.idempotency.PartnerIdempotencySpringDataRepository;
import com.sixpay.partner.infrastructure.outbox.OutboxEventSpringDataRepository;
import com.sixpay.partner.infrastructure.persistence.PartnerSpringDataRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class PartnerMvpSteps {

    private static final Pattern ID_PATTERN =
            Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([0-9a-fA-F-]{36})\\\"");

    private static final Pattern STATUS_PATTERN =
            Pattern.compile("\\\"status\\\"\\s*:\\s*\\\"([A-Z_]+)\\\"");

    @Autowired MockMvc mockMvc;
    @Autowired PartnerSpringDataRepository partnerRepository;
    @Autowired PartnerAuditSpringDataRepository auditRepository;
    @Autowired PartnerIdempotencySpringDataRepository idempotencyRepository;
    @Autowired OutboxEventSpringDataRepository outboxRepository;

    private PartnerScenarioState state;

    @Before
    public void reset() {
        state = new PartnerScenarioState();
        state.idempotencyKey = uniqueKey("create");
        state.partnerCountBefore = partnerRepository.count();
        state.auditCountBefore = auditRepository.count();
        state.idempotencyCountBefore = idempotencyRepository.count();
        state.outboxCountBefore = outboxRepository.count();
    }

    @Given("an administrator")
    public void anAdministrator() {}

    @Given("a pending partner exists")
    public void pendingPartnerExists() throws Exception {
        createPartner(uniqueKey("create-pending"));
        assertStatusCode(201);
        assertPartnerStatus("PENDING_VALIDATION");
    }

    @Given("an active partner exists")
    public void activePartnerExists() throws Exception {
        pendingPartnerExists();
        approvePartner(uniqueKey("approve"));
        assertStatusCode(200);
        assertPartnerStatus("ACTIVE");
    }

    @Given("a suspended partner exists")
    public void suspendedPartnerExists() throws Exception {
        activePartnerExists();
        suspendPartner(uniqueKey("suspend"));
        assertStatusCode(200);
        assertPartnerStatus("SUSPENDED");
    }

    @When("the administrator creates a valid partner")
    public void administratorCreatesValidPartner() throws Exception {
        createPartner(state.idempotencyKey);
    }

    @When("the administrator submits an invalid partner")
    public void administratorSubmitsInvalidPartner() throws Exception {
        var result = mockMvc.perform(post("/api/v1/partners")
                        .with(csrf())
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", uniqueKey("invalid"))
                        .content("{\"partnerIdentifier\":\"INVALID_PARTNER\",\"legalName\":\"\",\"technicalContactName\":\"Alice Ops\",\"technicalContactEmail\":\"not-an-email\",\"authorizedTransactionTypes\":[]}"))
                .andReturn();
        capture(result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @When("the manager approves the partner")
    public void managerApprovesPartner() throws Exception {
        approvePartner(uniqueKey("approve"));
    }

    @When("the manager rejects the partner")
    public void managerRejectsPartner() throws Exception {
        var result = mockMvc.perform(post("/api/v1/partners/{partnerId}/validation", state.partnerId)
                        .with(csrf())
                        .with(manager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", uniqueKey("reject"))
                        .header("X-Correlation-ID", uniqueKey("corr"))
                        .content("{\"decision\":\"REJECT\",\"reason\":\"MVP acceptance rejection\"}"))
                .andReturn();
        capture(result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @When("the manager tries to approve the already active partner")
    public void managerTriesToApproveAlreadyActivePartner() throws Exception {
        approvePartner(uniqueKey("approve-again"));
    }

    @When("the administrator suspends the partner")
    public void administratorSuspendsPartner() throws Exception {
        suspendPartner(uniqueKey("suspend"));
    }

    @When("the administrator reactivates the partner")
    public void administratorReactivatesPartner() throws Exception {
        var result = mockMvc.perform(post("/api/v1/partners/{partnerId}/reactivation", state.partnerId)
                        .with(csrf())
                        .with(admin())
                        .header("Idempotency-Key", uniqueKey("reactivate"))
                        .header("X-Correlation-ID", uniqueKey("corr")))
                .andReturn();
        capture(result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @When("the administrator configures a payment validation threshold")
    public void administratorConfiguresValidationThreshold() throws Exception {
        var result = mockMvc.perform(put("/api/v1/partners/{partnerId}/validation-thresholds/{transactionType}",
                                state.partnerId, "PAYMENT")
                        .with(csrf())
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", uniqueKey("threshold"))
                        .header("X-Correlation-ID", uniqueKey("corr"))
                        .content("{\"currency\":\"XAF\",\"amount\":100000,\"validationLevels\":2}"))
                .andReturn();
        capture(result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @When("the partner reads its own status")
    public void partnerReadsOwnStatus() throws Exception {
        var result = mockMvc.perform(get("/api/v1/partners/{partnerId}/status", state.partnerId)
                        .with(partner(state.partnerId)))
                .andReturn();
        capture(result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @When("another partner tries to read that status")
    public void anotherPartnerReadsStatus() throws Exception {
        var result = mockMvc.perform(get("/api/v1/partners/{partnerId}/status", state.partnerId)
                        .with(partner(UUID.randomUUID())))
                .andReturn();
        capture(result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @When("the administrator creates a partner for audit verification")
    public void createsPartnerForAuditVerification() throws Exception {
        state.auditCountBefore = auditRepository.count();
        createPartner(uniqueKey("audit"));
        state.auditCountAfter = auditRepository.count();
    }

    @When("the administrator replays the same creation with the same idempotency key")
    public void replaysSameCreation() throws Exception {
        String key = uniqueKey("idem");
        state.partnerCountBefore = partnerRepository.count();
        state.auditCountBefore = auditRepository.count();
        state.outboxCountBefore = outboxRepository.count();

        createPartner(key);
        assertStatusCode(201);
        UUID first = state.partnerId;

        createPartner(key);
        assertStatusCode(201);
        UUID second = state.partnerId;

        state.firstPartnerId = first;
        assertThat(second).isEqualTo(first);

        state.partnerCountAfter = partnerRepository.count();
        state.auditCountAfter = auditRepository.count();
        state.outboxCountAfter = outboxRepository.count();
    }

    @When("the administrator creates a partner for outbox verification")
    public void createsPartnerForOutboxVerification() throws Exception {
        state.outboxCountBefore = outboxRepository.count();
        createPartner(uniqueKey("outbox"));
        state.outboxCountAfter = outboxRepository.count();
    }

    @When("an invalid second approval is attempted")
    public void invalidSecondApprovalAttempted() throws Exception {
        state.partnerCountBefore = partnerRepository.count();
        state.auditCountBefore = auditRepository.count();
        state.idempotencyCountBefore = idempotencyRepository.count();
        state.outboxCountBefore = outboxRepository.count();
        state.statusBeforeFailure = currentPartnerStatus();

        approvePartner(uniqueKey("invalid-second-approval"));

        state.partnerCountAfter = partnerRepository.count();
        state.auditCountAfter = auditRepository.count();
        state.idempotencyCountAfter = idempotencyRepository.count();
        state.outboxCountAfter = outboxRepository.count();
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int expected) {
        assertStatusCode(expected);
    }

    @Then("a partner id is returned")
    public void partnerIdReturned() {
        assertThat(state.partnerId).isNotNull();
    }

    @Then("the partner status is {string}")
    public void partnerStatusIs(String expected) {
        assertPartnerStatus(expected);
    }

    @Then("the configured threshold is returned")
    public void configuredThresholdReturned() {
        assertThat(state.responseBody).contains("\"transactionType\":\"PAYMENT\"");
        assertThat(state.responseBody).contains("\"currency\":\"XAF\"");
        assertThat(state.responseBody).contains("\"validationLevels\":2");
    }

    @Then("one audit record has been added")
    public void oneAuditRecordAdded() {
        assertThat(state.auditCountAfter).isEqualTo(state.auditCountBefore + 1);
    }

    @Then("only one partner has been created")
    public void onlyOnePartnerCreated() {
        assertThat(state.partnerCountAfter).isEqualTo(state.partnerCountBefore + 1);
    }

    @Then("only one creation audit record has been added")
    public void onlyOneCreationAuditRecordAdded() {
        assertThat(state.auditCountAfter).isEqualTo(state.auditCountBefore + 1);
    }

    @Then("only one creation outbox event has been added")
    public void onlyOneCreationOutboxEventAdded() {
        assertThat(state.outboxCountAfter).isEqualTo(state.outboxCountBefore + 1);
    }

    @Then("one outbox event has been added")
    public void oneOutboxEventAdded() {
        assertThat(state.outboxCountAfter).isEqualTo(state.outboxCountBefore + 1);
    }

    @Then("the failed mutation leaves no partial persistence")
    public void failedMutationLeavesNoPartialPersistence() {
        assertThat(state.httpStatus).isEqualTo(422);
        assertThat(state.partnerCountAfter).isEqualTo(state.partnerCountBefore);
        assertThat(state.auditCountAfter).isEqualTo(state.auditCountBefore);
        assertThat(state.idempotencyCountAfter).isEqualTo(state.idempotencyCountBefore);
        assertThat(state.outboxCountAfter).isEqualTo(state.outboxCountBefore);
        assertThat(currentPartnerStatus()).isEqualTo(state.statusBeforeFailure);
    }

    private void createPartner(String idempotencyKey) throws Exception {
        var result = mockMvc.perform(post("/api/v1/partners")
                        .with(csrf())
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", uniqueKey("corr"))
                        .content("{\"partnerIdentifier\":\"" + idempotencyKey + "\",\"legalName\":\"Cucumber Payments\",\"technicalContactName\":\"Alice Ops\",\"technicalContactEmail\":\"alice.ops@example.com\",\"authorizedTransactionTypes\":[\"PAYMENT\"]}"))
                .andReturn();

        capture(result.getResponse().getStatus(), result.getResponse().getContentAsString());

        Matcher matcher = ID_PATTERN.matcher(state.responseBody);
        if (matcher.find()) {
            state.partnerId = UUID.fromString(matcher.group(1));
        }
    }

    private void approvePartner(String idempotencyKey) throws Exception {
        var result = mockMvc.perform(post("/api/v1/partners/{partnerId}/validation", state.partnerId)
                        .with(csrf())
                        .with(manager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", uniqueKey("corr"))
                        .content("{\"decision\":\"APPROVE\",\"reason\":\"MVP acceptance approval\"}"))
                .andReturn();
        capture(result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    private void suspendPartner(String idempotencyKey) throws Exception {
        var result = mockMvc.perform(post("/api/v1/partners/{partnerId}/suspension", state.partnerId)
                        .with(csrf())
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", uniqueKey("corr"))
                        .content("{\"reason\":\"MVP acceptance suspension\"}"))
                .andReturn();
        capture(result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    private String currentPartnerStatus() {
        return partnerRepository.findAggregateById(state.partnerId)
                .orElseThrow()
                .status()
                .name();
    }

    private void capture(int status, String body) {
        state.httpStatus = status;
        state.responseBody = body == null ? "" : body;
    }

    private void assertStatusCode(int expected) {
        assertThat(state.httpStatus).isEqualTo(expected);
    }

    private void assertPartnerStatus(String expected) {
        Matcher matcher = STATUS_PATTERN.matcher(state.responseBody);
        assertThat(matcher.find())
                .as("response contains a partner status: %s", state.responseBody)
                .isTrue();
        assertThat(matcher.group(1)).isEqualTo(expected);
    }

    private static RequestPostProcessor admin() {
        return user("admin@sixpay").roles("ADMIN");
    }

    private static RequestPostProcessor manager() {
        return user("manager@sixpay").roles("MANAGER");
    }

    private static RequestPostProcessor partner(UUID partnerId) {
        return user(partnerId.toString()).roles("PARTNER");
    }

    private static String uniqueKey(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
