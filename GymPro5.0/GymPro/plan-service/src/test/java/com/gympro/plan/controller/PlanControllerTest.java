package com.gympro.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gympro.plan.config.SecurityConfig;
import com.gympro.plan.entity.MembershipPlan;
import com.gympro.plan.entity.MemberSubscription;
import com.gympro.plan.exception.AccessDeniedException;
import com.gympro.plan.exception.GlobalExceptionHandler;
import com.gympro.plan.exception.PlanNotFoundException;
import com.gympro.plan.exception.SubscriptionNotFoundException;
import com.gympro.plan.service.PlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanService planService;

    @Autowired
    private ObjectMapper objectMapper;

    private MembershipPlan samplePlan;
    private MemberSubscription sampleSubscription;

    @BeforeEach
    void setUp() {
        samplePlan = new MembershipPlan();
        samplePlan.setId(1L);
        samplePlan.setPlanName("Gold Monthly");
        samplePlan.setDescription("Full access monthly plan");
        samplePlan.setDurationType("MONTHLY");
        samplePlan.setPrice(999.0);
        samplePlan.setDurationDays(30);
        samplePlan.setActive(true);

        sampleSubscription = new MemberSubscription();
        sampleSubscription.setId(10L);
        sampleSubscription.setMemberId(5L);
        sampleSubscription.setMemberEmail("member@gym.com");
        sampleSubscription.setPlanId(1L);
        sampleSubscription.setPlanName("Gold Monthly");
        sampleSubscription.setStartDate(LocalDate.now());
        sampleSubscription.setEndDate(LocalDate.now().plusDays(30));
        sampleSubscription.setStatus("ACTIVE");
    }

    // ── POST /plans ────────────────────────────────────────

    @Test
    void createPlan_ShouldReturn200_WhenAdminRole() throws Exception {
        when(planService.createPlan(any(MembershipPlan.class))).thenReturn(samplePlan);

        mockMvc.perform(post("/plans")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePlan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("Gold Monthly"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createPlan_ShouldReturn403_WhenNotAdmin() throws Exception {
        // Controller throws before calling service when role != ADMIN
        mockMvc.perform(post("/plans")
                .header("X-User-Role", "MEMBER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePlan)))
                .andExpect(status().isForbidden());

        verify(planService, never()).createPlan(any());
    }

    // ── GET /plans ─────────────────────────────────────────

    @Test
    void getAll_ShouldReturnActivePlans() throws Exception {
        when(planService.getActivePlans()).thenReturn(Arrays.asList(samplePlan));

        mockMvc.perform(get("/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].planName").value("Gold Monthly"));
    }

    @Test
    void getAll_ShouldReturnEmptyList_WhenNoActivePlans() throws Exception {
        when(planService.getActivePlans()).thenReturn(List.of());

        mockMvc.perform(get("/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /plans/{id} ────────────────────────────────────

    @Test
    void getById_ShouldReturnPlan_WhenExists() throws Exception {
        when(planService.getPlanById(1L)).thenReturn(samplePlan);

        mockMvc.perform(get("/plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.planName").value("Gold Monthly"));
    }

    @Test
    void getById_ShouldReturn404_WhenPlanNotFound() throws Exception {
        when(planService.getPlanById(99L)).thenThrow(new PlanNotFoundException(99L));

        mockMvc.perform(get("/plans/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── PUT /plans/{id} ────────────────────────────────────

    @Test
    void updatePlan_ShouldReturn200_WhenAdminRole() throws Exception {
        MembershipPlan updated = new MembershipPlan();
        updated.setPlanName("Platinum Monthly");
        updated.setDescription("Premium access");
        updated.setPrice(1499.0);

        when(planService.updatePlan(eq(1L), any(MembershipPlan.class))).thenReturn(updated);

        mockMvc.perform(put("/plans/1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("Platinum Monthly"))
                .andExpect(jsonPath("$.price").value(1499.0));
    }

    @Test
    void updatePlan_ShouldReturn403_WhenNotAdmin() throws Exception {
        // Controller throws before calling service when role != ADMIN
        mockMvc.perform(put("/plans/1")
                .header("X-User-Role", "MEMBER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePlan)))
                .andExpect(status().isForbidden());

        verify(planService, never()).updatePlan(any(), any());
    }

    @Test
    void updatePlan_ShouldReturn404_WhenPlanNotFound() throws Exception {
        when(planService.updatePlan(eq(99L), any(MembershipPlan.class)))
                .thenThrow(new PlanNotFoundException(99L));

        mockMvc.perform(put("/plans/99")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePlan)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /plans/{id} ─────────────────────────────────

    @Test
    void deactivatePlan_ShouldReturn200_WhenAdminRole() throws Exception {
        when(planService.deactivatePlan(1L)).thenReturn("Plan deactivated ✅");

        mockMvc.perform(delete("/plans/1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string("Plan deactivated ✅"));
    }

    @Test
    void deactivatePlan_ShouldReturn403_WhenNotAdmin() throws Exception {
        // Controller throws before calling service when role != ADMIN
        mockMvc.perform(delete("/plans/1")
                .header("X-User-Role", "MEMBER"))
                .andExpect(status().isForbidden());

        verify(planService, never()).deactivatePlan(any());
    }

    @Test
    void deactivatePlan_ShouldReturn404_WhenPlanNotFound() throws Exception {
        when(planService.deactivatePlan(99L)).thenThrow(new PlanNotFoundException(99L));

        mockMvc.perform(delete("/plans/99")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound());
    }

    // ── POST /plans/subscribe ──────────────────────────────

    @Test
    void subscribe_ShouldReturnActiveSubscription() throws Exception {
        when(planService.subscribe(5L, "member@gym.com", 1L)).thenReturn(sampleSubscription);

        mockMvc.perform(post("/plans/subscribe")
                .param("memberId", "5")
                .param("memberEmail", "member@gym.com")
                .param("planId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.memberId").value(5))
                .andExpect(jsonPath("$.planName").value("Gold Monthly"));
    }

    @Test
    void subscribe_ShouldReturn404_WhenPlanNotFound() throws Exception {
        when(planService.subscribe(5L, "member@gym.com", 99L))
                .thenThrow(new PlanNotFoundException(99L));

        mockMvc.perform(post("/plans/subscribe")
                .param("memberId", "5")
                .param("memberEmail", "member@gym.com")
                .param("planId", "99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /plans/my/{memberId} ───────────────────────────

    @Test
    void myPlans_ShouldReturnSubscriptionsForMember() throws Exception {
        when(planService.getMySubscriptions(5L)).thenReturn(Arrays.asList(sampleSubscription));

        mockMvc.perform(get("/plans/my/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].memberId").value(5));
    }

    @Test
    void myPlans_ShouldReturnEmptyList_WhenNoSubscriptions() throws Exception {
        when(planService.getMySubscriptions(99L)).thenReturn(List.of());

        mockMvc.perform(get("/plans/my/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── DELETE /plans/subscription/{subId} ────────────────

    @Test
    void cancelSubscription_ShouldReturnSuccessMessage() throws Exception {
        when(planService.cancelSubscription(10L)).thenReturn("Subscription cancelled ✅");

        mockMvc.perform(delete("/plans/subscription/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("Subscription cancelled ✅"));
    }

    @Test
    void cancelSubscription_ShouldReturn404_WhenNotFound() throws Exception {
        when(planService.cancelSubscription(99L)).thenThrow(new SubscriptionNotFoundException(99L));

        mockMvc.perform(delete("/plans/subscription/99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /plans/test ────────────────────────────────────

    @Test
    void testEndpoint_ShouldReturnWorkingMessage() throws Exception {
        mockMvc.perform(get("/plans/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Plan Service Working ✅"));
    }
}
