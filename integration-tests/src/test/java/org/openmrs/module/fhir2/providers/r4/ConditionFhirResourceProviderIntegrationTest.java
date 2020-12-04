/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.providers.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.fhir2.FhirConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

public class ConditionFhirResourceProviderIntegrationTest extends BaseFhirR4IntegrationTest<ConditionFhirResourceProvider, Condition> {
	
	private static final String CONDITION_UUID = "86sgf-1f7d-4394-a316-0a458edf28c4";
	
	private static final String OBS_CONDITION_INITIAL_DATA_XML = "org/openmrs/module/fhir2/api/dao/impl/FhirObsConditionDaoImplTest_initial_data.xml";
	
	private static final String CONDITION_SUBJECT_UUID = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String WRONG_CONDITION_UUID = "950d965d-a935-429f-945f-75a502a90188";
	
	private static final String JSON_CREATE_CONDITION_DOCUMENT = "org/openmrs/module/fhir2/providers/ConditionWebTest_create.json";
	
	@Getter(AccessLevel.PUBLIC)
	@Autowired
	private ConditionFhirResourceProvider resourceProvider;
	
	@Before
	public void setUp() throws Exception {
		super.setup();
		executeDataSet(OBS_CONDITION_INITIAL_DATA_XML);
	}
	
	@Test
	public void shouldReturnConditionAsJson() throws Exception {
		MockHttpServletResponse response = get("/Condition/" + CONDITION_UUID).accept(FhirMediaTypes.JSON).go();
		
		assertThat(response, isOk());
		assertThat(response.getContentType(), is(FhirMediaTypes.JSON.toString()));
		assertThat(response.getContentAsString(), notNullValue());
		
		Condition condition = readResponse(response);
		
		assertThat(condition, notNullValue());
		assertThat(condition.getIdElement().getIdPart(), equalTo(CONDITION_UUID));
		
		assertThat(condition.hasClinicalStatus(), is(true));
		assertThat(condition.getClinicalStatus().getCodingFirstRep().getSystem(),
		    equalTo(FhirConstants.CONDITION_CLINICAL_STATUS_SYSTEM_URI));
		assertThat(condition.getClinicalStatus().getCodingFirstRep().getCode(), equalTo("UNKNOWN"));
		
		assertThat(condition.getOnsetDateTimeType().getValue(),
		    equalTo(Date.from(LocalDateTime.of(2008, 07, 01, 0, 0, 0).atZone(ZoneId.systemDefault()).toInstant())));
		
		assertThat(condition.hasSubject(), is(true));
		assertThat(condition.getSubject().getReference(), equalTo("Patient/" + CONDITION_SUBJECT_UUID));
		
		assertThat(condition, validResource());
	}
	
	@Test
	public void shouldReturnNotFoundWhenConditionNotFoundAsJson() throws Exception {
		MockHttpServletResponse response = get("/Condition/" + WRONG_CONDITION_UUID).accept(FhirMediaTypes.JSON).go();
		
		assertThat(response, isNotFound());
		assertThat(response.getContentType(), is(FhirMediaTypes.JSON.toString()));
		assertThat(response.getContentAsString(), notNullValue());
		
		OperationOutcome operationOutcome = readOperationOutcome(response);
		
		assertThat(operationOutcome, notNullValue());
		assertThat(operationOutcome.hasIssue(), is(true));
	}
	
	@Test
	public void shouldReturnConditionAsXML() throws Exception {
		MockHttpServletResponse response = get("/Condition/" + CONDITION_UUID).accept(FhirMediaTypes.XML).go();
		
		assertThat(response, isOk());
		assertThat(response.getContentType(), is(FhirMediaTypes.XML.toString()));
		assertThat(response.getContentAsString(), notNullValue());
		
		Condition condition = readResponse(response);
		
		assertThat(condition, notNullValue());
		assertThat(condition.getIdElement().getIdPart(), equalTo(CONDITION_UUID));
		
		assertThat(condition.hasClinicalStatus(), is(true));
		assertThat(condition.getClinicalStatus().getCodingFirstRep().getSystem(),
		    equalTo(FhirConstants.CONDITION_CLINICAL_STATUS_SYSTEM_URI));
		assertThat(condition.getClinicalStatus().getCodingFirstRep().getCode(), equalTo("UNKNOWN"));
		
		assertThat(condition.getOnsetDateTimeType().getValue(),
		    equalTo(Date.from(LocalDateTime.of(2008, 07, 01, 0, 0, 0).atZone(ZoneId.systemDefault()).toInstant())));
		
		assertThat(condition.hasSubject(), is(true));
		assertThat(condition.getSubject().getReference(), equalTo("Patient/" + CONDITION_SUBJECT_UUID));
		
		assertThat(condition, validResource());
	}
	
	@Test
	public void shouldReturnNotFoundWhenConditionNotFoundAsXML() throws Exception {
		MockHttpServletResponse response = get("/Condition/" + WRONG_CONDITION_UUID).accept(FhirMediaTypes.XML).go();
		
		assertThat(response, isNotFound());
		assertThat(response.getContentType(), is(FhirMediaTypes.XML.toString()));
		assertThat(response.getContentAsString(), notNullValue());
		
		OperationOutcome operationOutcome = readOperationOutcome(response);
		
		assertThat(operationOutcome, notNullValue());
		assertThat(operationOutcome.hasIssue(), is(true));
	}
	
	@Test
	public void shouldCreateNewPatientAsJson() throws Exception {
		String jsonCondition;
		try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(JSON_CREATE_CONDITION_DOCUMENT)) {
			assertThat(is, notNullValue());
			jsonCondition = IOUtils.toString(is, StandardCharsets.UTF_8);
			assertThat(jsonCondition, notNullValue());
		}
		
		MockHttpServletResponse response = post("/Condition").accept(FhirMediaTypes.JSON).jsonContent(jsonCondition).go();
		
		assertThat(response, isCreated());
		assertThat(response.getHeader("Location"), containsString("/Condition/"));
		assertThat(response.getContentType(), is(FhirMediaTypes.JSON.toString()));
		assertThat(response.getContentType(), notNullValue());
		
		Condition condition = readResponse(response);
		
		assertThat(condition, notNullValue());
		assertThat(condition.getIdElement().getIdPart(), notNullValue());
		assertThat(condition.getClinicalStatus(), notNullValue());
		assertThat(condition.getClinicalStatus().getCodingFirstRep().getCode(), equalTo("UNKNOWN"));
		assertThat(condition.getCode(), notNullValue());
		assertThat(condition.getCode().getCoding(),
		    hasItem(hasProperty("code", equalTo("116128AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))));
		assertThat(condition.getSubject(), notNullValue());
		assertThat(condition.getSubject().getReference(), endsWith(CONDITION_SUBJECT_UUID));
		
		assertThat(condition, validResource());
		
		response = get("/Condition/" + condition.getIdElement().getIdPart()).accept(FhirMediaTypes.JSON).go();
		assertThat(response, isOk());
		Condition newCondition = readResponse(response);
		assertThat(newCondition.getId(), equalTo(condition.getId()));
	}
	
}