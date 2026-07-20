package org.opengroup.osdu.file.azure;

import org.opengroup.osdu.azure.util.AzureServicePrincipal;
import org.opengroup.osdu.file.HttpClient;

import java.io.IOException;
import com.google.common.base.Strings;

public class HttpClientAzure extends HttpClient {
  @Override
  public String getAccessToken() throws IOException {
    String bearerToken = System.getProperty("INTEGRATION_TESTER_ACCESS_TOKEN", System.getenv("INTEGRATION_TESTER_ACCESS_TOKEN"));
    if(!Strings.isNullOrEmpty(bearerToken) && Strings.isNullOrEmpty(accessToken)) {
      System.out.println("Using INTEGRATION_TESTER_ACCESS_TOKEN bearer token from environment variable");
      accessToken = bearerToken;
    }
    else if (Strings.isNullOrEmpty(accessToken)) {
      String sp_id = System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
      String sp_secret = System.getProperty("TESTER_SERVICEPRINCIPAL_SECRET", System.getenv("TESTER_SERVICEPRINCIPAL_SECRET"));
      String tenant_id = System.getProperty("AZURE_AD_TENANT_ID", System.getenv("AZURE_AD_TENANT_ID"));
      String app_resource_id = System.getProperty("AZURE_AD_APP_RESOURCE_ID", System.getenv("AZURE_AD_APP_RESOURCE_ID"));
      accessToken = new AzureServicePrincipal().getIdToken(sp_id, sp_secret, tenant_id, app_resource_id);
    }
    return "Bearer " + accessToken;
  }

  @Override
  public String getNoDataAccessToken() throws IOException {
    String bearerToken = System.getProperty("NO_DATA_ACCESS_TESTER_ACCESS_TOKEN", System.getenv("NO_DATA_ACCESS_TESTER_ACCESS_TOKEN"));
    if(!Strings.isNullOrEmpty(bearerToken) && Strings.isNullOrEmpty(noDataAccessToken)) {
      System.out.println("Using NO_DATA_ACCESS_TESTER_ACCESS_TOKEN bearer token from environment variable");
      noDataAccessToken = bearerToken;
    }
    else if (Strings.isNullOrEmpty(noDataAccessToken)) {
      String sp_id = System.getProperty("NO_DATA_ACCESS_TESTER", System.getenv("NO_DATA_ACCESS_TESTER"));
      String sp_secret = System.getProperty("NO_DATA_ACCESS_TESTER_SERVICEPRINCIPAL_SECRET", System.getenv("NO_DATA_ACCESS_TESTER_SERVICEPRINCIPAL_SECRET"));
      String tenant_id = System.getProperty("AZURE_AD_TENANT_ID", System.getenv("AZURE_AD_TENANT_ID"));
      String app_resource_id = System.getProperty("AZURE_AD_APP_RESOURCE_ID", System.getenv("AZURE_AD_APP_RESOURCE_ID"));
      noDataAccessToken = new AzureServicePrincipal().getIdToken(sp_id, sp_secret, tenant_id, app_resource_id);
    }
    return "Bearer " + noDataAccessToken;
  }
}
