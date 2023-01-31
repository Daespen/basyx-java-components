/*******************************************************************************
 * Copyright (C) 2023 the Eclipse BaSyx Authors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * SPDX-License-Identifier: MIT
 ******************************************************************************/

package org.eclipse.digitaltwin.basyx.submodelrepository.http;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.eclipse.digitaltwin.basyx.submodelservice.DummySubmodelFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Tests the Submodel specific parts of the SubmodelRepository HTTP/REST API
 * 
 * @author schnicke
 *
 */
public class TestSubmodelRepositorySubmodelHTTP {

	private String submodelAccessURL = "http://localhost:8080/submodels";

	private ConfigurableApplicationContext appContext;

	@Before
	public void startAASRepo() throws Exception {
		appContext = new SpringApplication(DummySubmodelRepositoryComponent.class).run(new String[] {});
	}

	@After
	public void shutdownAASRepo() {
		appContext.close();
	}

	@Test
	public void getAllSubmodelsPreconfigured() throws IOException, ParseException {
		String submodelsJSON = getAllSubmodelsJSON();
		String expectedSubmodelsJSON = getAllSubmodelJSON();
		SubmodelRepositoryHTTPTestUtils.assertSameJSONContent(expectedSubmodelsJSON, submodelsJSON);
	}

	@Test
	public void getSpecificSubmodel() throws ParseException, IOException {
		String submodelJSON = requestSpecificSubmodelJSON(DummySubmodelFactory.createTechnicalDataSubmodel().getId());
		String expectedSubmodelJSON = getSingleSubmodelJSON();

		SubmodelRepositoryHTTPTestUtils.assertSameJSONContent(expectedSubmodelJSON, submodelJSON);
	}

	@Test
	public void getSpecificSubmodelNonExisting() throws IOException {
		CloseableHttpResponse response = requestSubmodel("nonExisting");

		assertEquals(HttpStatus.NOT_FOUND.value(), response.getCode());
	}


	@Test
	public void updateExistingSubmodel() throws IOException, ParseException {
		String id = "7A7104BDAB57E184";
		String expectedSubmodelJSON = getUpdatedSubmodelJSON();

		CloseableHttpResponse creationResponse = putSubmodel(id, expectedSubmodelJSON);

		assertEquals(HttpStatus.NO_CONTENT.value(), creationResponse.getCode());

		String submodelJSON = requestSpecificSubmodelJSON(id);
		SubmodelRepositoryHTTPTestUtils.assertSameJSONContent(expectedSubmodelJSON, submodelJSON);
	}

	@Test
	public void updateNonExistingSubmodel() throws IOException {
		String id = "nonExisting";
		String expectedSubmodelJSON = getUpdatedSubmodelJSON();

		CloseableHttpResponse updateResponse = putSubmodel(id, expectedSubmodelJSON);

		assertEquals(HttpStatus.NOT_FOUND.value(), updateResponse.getCode());
	}

	@Test
	public void createSubmodelNewId() throws IOException, ParseException {
		String submodelJSON = getNewSubmodelJSON();
		CloseableHttpResponse creationResponse = createSubmodel(submodelJSON);

		assertSubmodelCreationReponse(submodelJSON, creationResponse);

		String requestedSubmodel = requestSpecificSubmodelJSON("newSubmodel");
		SubmodelRepositoryHTTPTestUtils.assertSameJSONContent(submodelJSON, requestedSubmodel);
	}

	@Test
	public void createSubmodelCollidingId() throws IOException {
		String submodelJSON = getSingleSubmodelJSON();
		CloseableHttpResponse creationResponse = createSubmodel(submodelJSON);

		assertEquals(HttpStatus.CONFLICT.value(), creationResponse.getCode());
	}

	


	private void assertSubmodelCreationReponse(String submodelJSON, CloseableHttpResponse creationResponse) throws IOException, ParseException, JsonProcessingException, JsonMappingException {
		assertEquals(HttpStatus.CREATED.value(), creationResponse.getCode());
		String response = SubmodelRepositoryHTTPTestUtils.getResponseAsString(creationResponse);
		SubmodelRepositoryHTTPTestUtils.assertSameJSONContent(submodelJSON, response);
	}

	private CloseableHttpResponse createSubmodel(String submodelJSON) throws IOException {
		HttpPost submodelCreationRequest = new HttpPost(submodelAccessURL);
		submodelCreationRequest.setHeader("Content-type", "application/json");

		StringEntity smEntity = new StringEntity(submodelJSON);
		submodelCreationRequest.setEntity(smEntity);

		CloseableHttpClient client = HttpClients.createDefault();
		return client.execute(submodelCreationRequest);
	}

	private CloseableHttpResponse putSubmodel(String id, String submodelJSON) throws IOException {
		HttpPut submodelUpdateRequest = new HttpPut(submodelAccessURL + "/" + id + "/submodel");
		submodelUpdateRequest.setHeader("Content-type", "application/json");

		StringEntity smEntity = new StringEntity(submodelJSON);
		submodelUpdateRequest.setEntity(smEntity);

		CloseableHttpClient client = HttpClients.createDefault();
		return client.execute(submodelUpdateRequest);
	}

	private String requestSpecificSubmodelJSON(String id) throws IOException, ParseException {
		CloseableHttpResponse response = requestSubmodel(id);

		return SubmodelRepositoryHTTPTestUtils.getResponseAsString(response);
	}

	private CloseableHttpResponse requestSubmodel(String id) throws IOException {
		return SubmodelRepositoryHTTPTestUtils.executeGetOnURL(submodelAccessURL + "/" + id + "/submodel");
	}

	private String getAllSubmodelsJSON() throws IOException, ParseException {
		CloseableHttpResponse response = SubmodelRepositoryHTTPTestUtils.executeGetOnURL(submodelAccessURL);

		return SubmodelRepositoryHTTPTestUtils.getResponseAsString(response);
	}

	private String getUpdatedSubmodelJSON() throws IOException {
		return SubmodelRepositoryHTTPTestUtils.readJSONStringFromFile("classpath:SingleSubmodelUpdate.json");
	}

	private String getNewSubmodelJSON() throws IOException {
		return SubmodelRepositoryHTTPTestUtils.readJSONStringFromFile("classpath:SingleSubmodelNew.json");
	}

	private String getSingleSubmodelJSON() throws IOException {
		return SubmodelRepositoryHTTPTestUtils.readJSONStringFromFile("classpath:SingleSubmodel.json");
	}

	private String getAllSubmodelJSON() throws IOException {
		return SubmodelRepositoryHTTPTestUtils.readJSONStringFromFile("classpath:MultipleSubmodels.json");
	}

}