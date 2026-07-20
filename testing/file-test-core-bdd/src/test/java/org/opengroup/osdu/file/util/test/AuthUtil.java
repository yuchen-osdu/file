package org.opengroup.osdu.file.util.test;

import com.google.common.base.Strings;
import org.opengroup.osdu.azure.util.AzureServicePrincipal;

public class AuthUtil {
  public synchronized String getToken() throws Exception {
    String token = null;
    String vendor = System.getProperty("VENDOR", System.getenv("VENDOR"));
    if (Strings.isNullOrEmpty(token) && vendor.equals("gcp")) {
      String serviceAccountFile = System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
      token = new GoogleServiceAccount(serviceAccountFile).getAuthToken();
    } else if ("aws".equals(vendor)) {
      System.out.println("Token generation code for aws comes here");
    } else if ("azure".equals(vendor)) {
      String bearerToken = System.getProperty("INTEGRATION_TESTER_ACCESS_TOKEN", System.getenv("INTEGRATION_TESTER_ACCESS_TOKEN"));
      if(!Strings.isNullOrEmpty(bearerToken)) {
        System.out.println("Using bearer token from environment variable");
        token = bearerToken;
      }
      else {
        System.out.println("Generating bearer token using SPN client id and secret");
        String sp_id = System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
        String sp_secret = System.getProperty("TESTER_SERVICEPRINCIPAL_SECRET", System.getenv("TESTER_SERVICEPRINCIPAL_SECRET"));
        String tenant_id = System.getProperty("AZURE_AD_TENANT_ID", System.getenv("AZURE_AD_TENANT_ID"));
        String app_resource_id = System.getProperty("AZURE_AD_APP_RESOURCE_ID", System.getenv("AZURE_AD_APP_RESOURCE_ID"));
        token = new AzureServicePrincipal().getIdToken(sp_id, sp_secret, tenant_id, app_resource_id);
      }
    } else if ("ibm".equals(vendor)) {
      System.out.println("Token generation code for ibm comes here");
    }
    System.out.println("Bearer " + token);
    return "Bearer " + token;
  }
}
