package org.swaggerhub;

import com.google.gson.*;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static String logfile = ("default_log_" + LocalDateTime.now() + ".txt").replace(":", "-");
    public static String systemSeparator = System.getProperty("file.separator");
    public static void main(String[] args) {

        String response;
        Scanner input = new Scanner(System.in);

        String host;
        String key;

        do {
            System.out.println("Enter admin API key of your SwaggerHub instance");
            key = input.nextLine();
            System.out.println("Enter host with protocol of your SwaggerHub instance");
            host = input.nextLine().toLowerCase();

            if (!CheckHost(host))
            {
                System.out.println("Bad host input.");
            }
        }while (!CheckHost(host));

        System.out.println("Would you like to import or export SwaggerHub data? (import/export)");
        response = input.nextLine().toLowerCase();
        input.close();

        if (response.equals("export"))
        {
            logfile = logfile.replace("default", "export");
            System.out.println("Writing log file to " + logfile);
            if (!TestConnection(host, key)) return;
            ExportDocuments(host, key);
            ExportIntegrations(host, key);
            ExportProjects(host, key);

        }else if (response.equals("import"))
        {
            logfile = logfile.replace("default", "import");
            if (!TestConnection(host, key)) return;
            System.out.println("Writing log file to " + logfile);
            ImportDocuments(host, key);
            ImportIntegrations(host, key);
            ImportProjects(host, key);
        }else{
            System.out.println("Unaccepted response.");
            return;
        }

    }

    public static boolean CheckHost(String host)
    {
        if (host.contains("http://"))
        {
            if (host.indexOf("http://") != 0)
            {
                return false;
            }
        }
        if (host.contains("https://"))
        {
            if (host.indexOf("https://") != 0)
            {
                return false;
            }
        }
        if (host.equals("http://") || host.equals("https://"))
        {
            return false;
        }
        if (host.trim().isEmpty())
        {
            return false;
        }
        if (host.endsWith("/"))
        {
            System.out.println("Host should not end with a slash /.\n");
            return false;
        }
        return true;
    }

    public static boolean TestConnection(String host, String key)
    {
        String url = String.format("%s/v1/specs", host);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers(
                        "Accept", "application/json",
                        "Authorization", key
                )
                .GET()
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();

        try {
            SaveLog("Testing connection to the host " + host + " ...");
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200){
                SaveLog("Connection to the host successful.");
            }
            else{
                SaveLog("Connection to the host failed. Status code: " + response.statusCode());
                SaveLog(response.body());
                return false;
            }

        } catch (IOException | InterruptedException | IllegalArgumentException exception) {
            SaveLog(exception.getMessage());
            return false;
        }

        httpClient.close();
        return true;
    }
    public static void SaveLog(String message){
        System.out.println(message);
        try(FileWriter fileWriter = new FileWriter(logfile, true)){
            message+= "\n";
            fileWriter.append(message);
            fileWriter.close();
        }
        catch(IOException exception){
            System.out.println("Error writing file: " + exception.getMessage());
        }
    }

    public static void ExportDocuments(String host, String key)
    {
        int totalCount = GetSpecs(key, host, 0, "ANY").totalCount;
        int apiVersionsCount = 0;
        int domainVersionCount = 0;
        int templateVersionCount = 0;

        if (totalCount == 0)
        {
            SaveLog("No documents to export from the SwaggerHub instance.");
            return;
        }
        SaveLog("Exporing APIs, domains, templates...");
        SaveLog("Total unique documents: " + totalCount);
        HttpClient httpClient = HttpClient.newHttpClient();

        for (int i = 0; i < totalCount; i++) {

            Specs spec = GetSpecs(key, host, i, "ANY");
            int propertiesCount = spec.apis[0].properties.length;
            String x_versions = "";
            for (int j = 0; j < propertiesCount; j++) {
                if (spec.apis[0].properties[j].type.equals("X-Versions")) {
                    x_versions = spec.apis[0].properties[j].value;
                    break;
                }
            }
            String[] versions = x_versions.split(",");

            for (String s : versions) {

                String[] URL_split = spec.apis[0].properties[0].url.split("/");
                if (s.charAt(0) == '-' || s.charAt(0) == '*') {
                    s = s.substring(1);
                }
                String URL = String.format("%s//%s/%s/%s/%s/%s/%s",
                        URL_split[0],
                        URL_split[2],
                        URL_split[3],
                        URL_split[4],
                        URL_split[5],
                        URL_split[6],
                        s);
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(URL))
                        .headers(
                                "Accept", "application/yaml",
                                "Authorization", key
                        )
                        .GET()
                        .build();

                HttpResponse<String> response;

                String[] substrings = URL.split("/");
                String documentName = substrings[6];
                String version = substrings[7];
                String organisationName = substrings[5];
                String type = substrings[4];

                String path = String.format("SwaggerHub-Downloads\\%s\\%s\\",
                        organisationName,
                        substrings[4]);
                path = path.replace("\\", systemSeparator);
                String filename = String.format("%s-version_%s.yaml",
                        documentName,
                        version);

                if (type.equals("apis")) apiVersionsCount ++;
                if (type.equals("domains")) domainVersionCount ++;
                if (type.equals("templates")) templateVersionCount ++;

                try
                {
                    response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200)
                    {
                        SaveDocumentToFile(response.body(), path, filename);
                    }
                    else
                    {
                        System.out.println("Something went wrong: " + response.statusCode());
                        System.out.println(response.body());
                        return;
                    }
                }
                catch (IOException | InterruptedException exception)
                {
                    System.out.println(exception.getMessage());
                }

            }
        }
        httpClient.close();
        SaveLog("Exporting documents completed.");
        SaveLog("Exported " + apiVersionsCount + " API versions.");
        SaveLog("Exported " + domainVersionCount + " domain versions.");
        SaveLog("Exported " + templateVersionCount + " template versions.\n");

    }

    public static void ExportIntegrations(String host, String key)
    {
        Specs startSpec = GetSpecs(key, host, 0, "API");
        int totalCount = startSpec.totalCount;

        SaveLog("Exporting integrations...");

        if (totalCount == 0)
        {
            return;
        }
        HttpClient httpClient = HttpClient.newHttpClient();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        for (int i = 0 ; i < totalCount; i ++)
        {
            Specs spec = GetSpecs (key, host, i, "API");
            int propertiesCount = spec.apis[0].properties.length;
            String x_versions = "";

            for (int j = 0; j < propertiesCount; j++)
            {
                if (spec.apis[0].properties[j].type.equals("X-Versions"))
                {
                    x_versions = spec.apis[0].properties[j].value;
                    break;
                }
            }

            String[] versions = x_versions.split(",");
            for (String s : versions) {

                String[] URL_split = spec.apis[0].properties[0].url.split("/");
                if (s.charAt(0) == '-' || s.charAt(0) == '*') {
                    s = s.substring(1);
                }
                String URL = String.format("%s//%s/%s/%s/%s/%s/%s",
                        URL_split[0],
                        URL_split[2],
                        URL_split[3],
                        URL_split[4],
                        URL_split[5],
                        URL_split[6],
                        s);

                String[] substrings = URL.split("/");
                String documentName = substrings[6];
                String version = substrings[7];
                String organisationName = substrings[5];

                String getURL = String.format("%s/v1/apis/%s/%s/%s/integrations",
                        host, organisationName, documentName, version);
                HttpRequest httpGETRequest = HttpRequest.newBuilder()
                        .uri(URI.create(getURL))
                        .headers(
                                "Accept", "application/json",
                                "Authorization", key
                        )
                        .GET()
                        .build();

                HttpResponse<String> response;
                String path = String.format("SwaggerHub-Downloads\\%s\\integrations\\",
                        organisationName);
                path = path.replace("\\", systemSeparator);
                String filename = String.format("%s-version_%s.json",
                        documentName,
                        version);

                try {
                    response = httpClient.send(httpGETRequest, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200)
                    {
                        JsonElement jsonElement = JsonParser.parseString(response.body());
                        SaveDocumentToFile(gson.toJson(jsonElement), path, filename);
                    }else
                    {
                        System.out.println("Something went wrong: " + response.statusCode());
                    }

                } catch (IOException | InterruptedException exception)
                {
                    System.out.println(exception.getMessage());
                }
            }
        }
        httpClient.close();
        SaveLog("Finished exporting integrations.\n");
    }

    public static void ExportProjects(String host, String key)
    {
        SaveLog("Exporting projects...");

        Orgs organizations = GetOrgs(key, host, 0);
        int totalCount = organizations.totalCount;
        if (totalCount == 0)
        {
            System.out.println("Couldn't find any organizations.");
            return;
        }
        ArrayList<String> organizationNames = new ArrayList<String>();

        for (int i = 0; i < totalCount; i ++)
        {
            Orgs org = GetOrgs(key, host, i);
            for (int j = 0; j < org.items.length; j++)
            {
                organizationNames.add(org.items[j].name);
            }
        }

        if (organizationNames.isEmpty())
        {
            return;
        }
        Projects projects = GetProjects(key, host, organizationNames.getFirst(), 1);//this endpoint should be iterated from index 1
        int totalProjectCount = projects.totalCount;

        if (totalProjectCount == 0) return;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        int page = 1;
        for (int i = 0; i < organizationNames.size(); i ++)
        {
            for (int j = 0; j < totalProjectCount; j ++)
            {
                Projects project = GetProjects(key, host, organizationNames.get(i), page);
                if (j >= project.projects.length)
                {
                    break;
                }

                ProjectsObject projectsObject = project.projects[j];
                JsonElement projectElement = gson.toJsonTree(projectsObject);
                String path = String.format("SwaggerHub-Downloads\\%s\\projects\\",
                            organizationNames.get(i));
                path = path.replace("\\", System.getProperty("file.separator"));
                String filename = project.projects[j].name + ".json";
                SaveDocumentToFile(gson.toJson(projectElement), path, filename);
                //if more projects than 100 which is the maximum -> increment page value and reset the loop
                if(j >= 99)
                {
                    page ++;
                    j = 0;
                }
            }
        }
        SaveLog("Finished exporting projects.\n");
    }

    public static void SaveDocumentToFile(String content, String path, String filename){
        new File(path).mkdirs();

        try(FileWriter fileWriter = new FileWriter(path + filename)){
            fileWriter.write(content);
            fileWriter.close();
            SaveLog("File saved to: " + path + filename);
        }
        catch(IOException exception){
            System.out.println("Error writing file: " + exception.getMessage());
        }
    }

    public static void ImportDocuments(String host, String key)
    {
        String currentPath = System.getProperty("user.dir");

        File mainPathFile = new File(currentPath);
        String[] mainDirectories = mainPathFile.list();
        if (!Arrays.toString(mainDirectories).contains("SwaggerHub-Downloads"))
        {
            System.out.println("Couldn't find SwaggerHub-Download directory.");
            return;
        }
        String backupPath = currentPath + "\\SwaggerHub-Downloads";
        backupPath = backupPath.replace("\\", systemSeparator);
        File backupPathFile = new File(backupPath);

        String[] backupDirectories = backupPathFile.list();

        if (backupDirectories == null) return;

        HttpClient httpClient = HttpClient.newHttpClient();

        SaveLog("Importing documents...");

        for (String s : backupDirectories){

            CreateDocument(host, key, backupPath, httpClient, s, "apis");
            CreateDocument(host, key, backupPath, httpClient, s, "domains");
            CreateDocument(host, key, backupPath, httpClient, s, "templates");
        }

        httpClient.close();
        SaveLog("Finished importing documents.\n");
    }

    public static void CreateDocument(String host, String key, String backupPath, HttpClient httpClient, String organization, String type) {
        String tempPath = String.format("%s\\%s", backupPath, organization);
        tempPath = tempPath.replace("\\", systemSeparator);
        File tempPathFile = new File(tempPath);
        String[] documentTypeDirectories = tempPathFile.list();

        if (documentTypeDirectories == null) return;

        if (documentTypeDirectories.length == 0) return;

        if (Arrays.toString(documentTypeDirectories).contains(type)) {

            tempPath = String.format("%s\\%s\\%s", backupPath, organization, type);
            tempPath = tempPath.replace("\\", systemSeparator);
            File documentDirectory = new File(tempPath);
            File[] documentFiles = documentDirectory.listFiles();
            List<String> documentFileNames = new ArrayList<String>();
            List<String> documentNames = new ArrayList<String>();
            List<String> documentVersions = new ArrayList<String>();

            if (documentFiles == null) return;

            for (File file : documentFiles) {
                documentFileNames.add(file.getName());
                documentNames.add(file.getName().split("-version_")[0]);
                documentVersions.add(file.getName().split("-version_")[1].split(".yaml")[0]);
            }

            for (int i = 0; i < documentFileNames.size(); i++) {
                String URL = String.format("%s/v1/%s/%s/%s?isPrivate=true&version=%s",
                        host, type, organization, documentNames.get(i), documentVersions.get(i));
                Path filePath = Path.of(documentFiles[i].getAbsolutePath());
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(URL))
                            .headers(
                                    "Content-Type", "application/yaml",
                                    "Authorization", key
                            )
                            .POST(HttpRequest.BodyPublishers.ofFile(filePath))
                            .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200 || response.statusCode() == 201)
                    {
                        SaveLog("Successfully created document: "
                                + documentNames.get(i)
                                + " version "
                                + documentVersions.get(i)
                                + " in "
                                + organization
                                + " organization."
                        );
                    }
                    else
                    {
                        SaveLog("Failed to create document: "
                                + documentNames.get(i)
                                + " version " +
                                documentVersions.get(i)
                                + " in "
                                + organization
                                + " organization: "
                                + response.body());
                    }
                } catch (IOException | InterruptedException exception) {
                    System.out.println("Something went wrong: " + exception.getMessage());;
                }
            }
        }
    }

    public static void ImportIntegrations(String host, String key)
    {
        String currentPath = System.getProperty("user.dir");
        File mainPathFile = new File(currentPath);
        String[] mainDirectories = mainPathFile.list();

        if (!Arrays.toString(mainDirectories).contains("SwaggerHub-Downloads"))
        {
            System.out.println("Couldn't find SwaggerHub-Download directory.");
            return;
        }
        String backupPath = currentPath + "\\SwaggerHub-Downloads";
        backupPath = backupPath.replace("\\", systemSeparator);
        File backupPathFile = new File(backupPath);

        String[] backupDirectories = backupPathFile.list();

        if (backupDirectories == null) return;

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response;

        SaveLog("Importing integrations...");
        for (String s : backupDirectories)
        {
            String tempPath = String.format("%s\\%s\\integrations",
                    backupPath,
                    s);
            tempPath = tempPath.replace("\\", systemSeparator);
            File[] integrationFiles = new File(tempPath).listFiles();

            if (integrationFiles == null) return;

            if (integrationFiles.length == 0) return;

            for (File f : integrationFiles)
            {
                String[] splitFilename = f.getName().split("-version_");

                if (splitFilename.length == 0 ) return;

                String apiName = splitFilename[0];
                String apiVersion = splitFilename[1].replace(".json","");

                String URL = String.format("%s/v1/apis/%s/%s/%s/integrations",
                        host, s, apiName, apiVersion);

                try
                {
                    JsonElement jsonElement = JsonParser.parseReader(new FileReader(f));
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    JsonArray integrations = jsonObject.getAsJsonArray("integrations");

                    for (JsonElement integration : integrations)
                    {
                        HttpRequest httpRequest = HttpRequest.newBuilder()
                                .uri(URI.create(URL))
                                .headers(
                                        "Content-Type", "application/json",
                                        "Authorization", key
                                )
                                .POST(HttpRequest.BodyPublishers.ofString(integration.toString()))
                                .build();
                        response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                        JsonObject integrationObject = integration.getAsJsonObject();
                        String integrationName = integrationObject.get("name").getAsString();

                        if (response.statusCode() == 200 | response.statusCode() == 201)
                        {
                            SaveLog("Successfully created integration " +
                                    integrationName +
                                    " in API " +
                                    apiName +
                                    " version " +
                                    apiVersion +
                                    " in " +
                                    s +
                                    " organization.");
                        }
                        else
                        {
                            SaveLog("Failed to create integration " +
                                    integrationName +
                                    " in API " +
                                    apiName +
                                    " version " +
                                    apiVersion +
                                    " in " +
                                    s +
                                    " organization: " +
                                    response.body());
                        }
                    }
                }catch (IOException | InterruptedException exception )
                {
                    System.out.println("Something went wrong: " + exception.getMessage());
                }
            }

        }

        httpClient.close();
        SaveLog("Finished importing integrations.\n");
    }

    public static void ImportProjects(String host, String key)
    {
        String currentPath = System.getProperty("user.dir");
        File mainPathFile = new File(currentPath);
        String[] mainDirectories = mainPathFile.list();

        if (!Arrays.toString(mainDirectories).contains("SwaggerHub-Downloads"))
        {
            System.out.println("Couldn't find SwaggerHub-Download directory.");
            return;
        }
        String backupPath = currentPath + "\\SwaggerHub-Downloads";
        backupPath = backupPath.replace("\\", systemSeparator);
        File backupPathFile = new File(backupPath);

        String[] backupDirectories = backupPathFile.list();

        if (backupDirectories == null) return;

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response;

        SaveLog("Importing projects...");

        for (String s : backupDirectories)
        {
            String tempPath = String.format("%s\\%s\\projects",
                    backupPath,
                    s);
            tempPath = tempPath.replace("\\", systemSeparator);
            File[] projectFiles = new File(tempPath).listFiles();

            if (projectFiles == null) return;

            if (projectFiles.length == 0) return;

            for (File f : projectFiles)
            {
                String projectName = f.getName().replace(".json", "");
                String URL = String.format("%s/v1/projects/%s",
                        host, s);

                try
                {
                    JsonElement jsonElement = JsonParser.parseReader(new FileReader(f));
                    JsonObject jsonObject = jsonElement.getAsJsonObject();

                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(URL))
                            .headers(
                                    "Content-Type", "application/json",
                                    "Authorization", key
                            )
                            .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                            .build();
                    response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200 || response.statusCode() == 201)
                    {
                        SaveLog("Successfully created project " +
                                projectName +
                                " in organization " +
                                s);
                    }else
                    {
                        SaveLog("Failed to create project " +
                                projectName +
                                " in organization " +
                                s +
                                " :" +
                                response.body());
                    }
                }catch (IOException | InterruptedException exception )
                {
                    System.out.println("Something went wrong: " + exception.getMessage());
                }
            }
        }
        httpClient.close();
        SaveLog("Finished importing projects.");
    }
    public static Specs GetSpecs (String key, String host, int page, String type){

        String URL = String.format("%s/v1/specs?specType=%s&visibility=ANY&state=ALL&page=%d&limit=1&sort=NAME&order=ASC",
                host, type, page);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .headers(
                        "Accept", "application/json",
                        "Authorization", key
                )
                .GET()
                .build();

        HttpResponse<String> response;
        Specs apiResponse = new Specs();

        try{
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            apiResponse = gson.fromJson(response.body(), Specs.class);
            httpClient.close();

        }catch(IOException | InterruptedException exception){
            System.out.println(exception.getMessage());
        }

        return apiResponse;
    }

    public static Orgs GetOrgs(String key, String host, int page)
    {
        String getOrganizationsURL = String.format("%s/api/user-management/v1/orgs?sortBy=NAME&order=ASC&page=%d&pageSize=1",
                host, page);

        HttpRequest httpGETRequest = HttpRequest.newBuilder()
                .uri(URI.create(getOrganizationsURL))
                .headers(
                        "Accept", "application/json",
                        "Authorization", key
                )
                .GET()
                .build();

        HttpResponse<String> response;
        HttpClient httpClient = HttpClient.newHttpClient();
        Orgs apiResponse = new Orgs();
        try
        {
            response = httpClient.send(httpGETRequest, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            apiResponse = gson.fromJson(response.body(), Orgs.class);
            httpClient.close();

        }catch(IOException | InterruptedException exception)
        {
            System.out.println(exception.getMessage());
        }
        return apiResponse;
    }

    public static Projects GetProjects(String key, String host, String organizationName, int page)
    {
        String getProjectsURL = String.format("%s/v1/projects/%s?nameOnly=false&page=%d&limit=100&order=ASC",
                host, organizationName, page);

        HttpRequest httpGETRequest = HttpRequest.newBuilder()
                .uri(URI.create(getProjectsURL))
                .headers(
                        "Accept", "application/json",
                        "Authorization", key
                )
                .GET()
                .build();

        HttpResponse<String> response;
        HttpClient httpClient = HttpClient.newHttpClient();
        Projects apiResponse = new Projects();
        try
        {
            response = httpClient.send(httpGETRequest, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            apiResponse = gson.fromJson(response.body(), Projects.class);
            httpClient.close();

        }catch(IOException | InterruptedException exception)
        {
            System.out.println(exception.getMessage());
        }
        return apiResponse;
    }
}

