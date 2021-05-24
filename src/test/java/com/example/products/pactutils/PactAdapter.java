package com.example.products.pactutils;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PactAdapter {
  public void writePact(List<ServeEvent> events) {
    ObjectMapper mapper = new ObjectMapper();
    Pact pact = new Pact();

    try {
      for (ServeEvent e : events) {
        Interaction interaction = new Interaction();

        interaction.setDescription(e.getRequest().getMethod() + "_" + e.getRequest().getUrl() + "_" + e.getId());
        interaction.request.setMethod(e.getRequest().getMethod().toString());
        interaction.request.setPath(e.getRequest().getUrl());

        if (e.getStubMapping().getRequest().getHeaders() != null) {
          System.out.println("Headers" + e.getRequest().getHeaders());
          interaction.request.headers = new HashMap<>();
          e.getStubMapping().getRequest().getHeaders().forEach((h, v) -> {
            interaction.request.headers.put(h.toString(), v.getExpected());
          });
        }

        if (e.getRequest().getBodyAsString() != null && e.getRequest().getBody().length > 0) {
          interaction.request.setBody(e.getRequest().getBodyAsString());
        }

        if (e.getStubMapping().getResponse().getHeaders() != null) {
          Map<String, String> headers = new HashMap<>();
          e.getStubMapping().getResponse().getHeaders().all().forEach((h) -> {
            headers.put(h.caseInsensitiveKey().toString(), String.join(",", h.values()));
          });
          interaction.response.setHeaders(headers);
        }
        interaction.response.setStatus(e.getStubMapping().getResponse().getStatus());
        if (e.getStubMapping().getResponse().specifiesBodyContent()) {
          interaction.response.setBody(e.getStubMapping().getResponse().getBody());
        }

        pact.interactions.add(interaction);
      }

      pact.consumer = new Pacticipant("pactflow-example-consumer-wiremock");
      String provider = System.getenv().getOrDefault("PACT_PROVIDER", "pactflow-example-provider-restassured");
      pact.provider = new Pacticipant(provider);

      File dir = new File("build/pacts");
      if (!dir.exists()) {
        dir.mkdir();
      }
      mapper.writeValue(new File("build/pacts/" + pact.consumer.getName() + "-" + pact.provider.getName() + ".json"), pact);

    } catch (IOException e) {
        e.printStackTrace();
    }
  }
}
