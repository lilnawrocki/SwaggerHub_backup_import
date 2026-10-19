## Description
Java console application for downloading and importing data from and to SwaggerHub on-premise instance. This version of the application is a variant of [`SwaggerHub_data_migration`](https://github.com/lilnawrocki/SwaggerHub_data_migration) that allows downloading and importing as separate steps and reusing the same host. The application was tested on versions 2.10.1 and 2.15.2.

### Instructions
The application will ask for admin API key, host with protocol, and ask whether you would like to import or export data. Exported data will be stored in `SwaggerHub-Downloads` directory. Import option will be using this directory to upload data to the specified host. For each run, a log file named `export_log_DATE` or `import_log_DATE` will be created in the project directory.

### Important notes
This application contains various simplifications in code which may result in unexpected results. To use it properly, follow the steps below:
1. Run the application using the `export` option. This will save all documents (APIs, domains, templates), integration settings, and projects to `SwaggerHub-Downloads` inside the project directory.
2. Create a .zip file of the `SwaggerHub-Downloads` folder and name it, for example, `Export_SwaggerHub-2.10.1_DATE.zip` and move it to a safe place.
3. On the SwaggerHub instance where you plan to import the data, recreate manually organisations.
4. Run the application using the `import` option, specifying the host of the target instance. This operation will read all directories within `SwaggerHub-Downloads` and import all documents.

- If `SwaggerHub-Downloads` exists within the project, the export option will overwrite any existing files and add new ones.
- `SwaggerHub-Downloads` should not be renamed or moved for import to work properly. This directory name and path are hardcoded.
- If you would like to reuse the application to export & import from and to another instance, you need to make sure that the previous downloads are zipped and saved in a safe location, and the directory `SwaggerHub-Downloads` does not exist, otherwise, the `export` run will create additional organisation folders and `import` will try send all this data to the specified host. This happens because the application is simply scanning all folders inside `SwaggerHub-Downloads`, it does not know which organisations should be imported and which should not be.
- Document names should at best not contain `-version_` string as this is used to properly split the file name into document name and version.
- There is a known limitation of the GET integrations endpoint which returns incomplete JSON for some integrations, such as webhook or GitHub sync. This may result in failures on importing integrations and cannot be fixed with code in this application. See import output below.
- It was observed on SwaggerHub 2.15.2 that if the documents already exist on the target instance during the import operation, the POST request will return bad status code with empty response body. The application will write such failures to the logs.

### Sample output
Export run:
```
Testing connection to the host 10.200.30.32.nip.io ...
Connection to the host successful.
Exporing APIs, domains, templates...
Total unique documents: 8
File saved to: SwaggerHub-Downloads\SmartBear\domains\domain-version_1.0.0.yaml
File saved to: SwaggerHub-Downloads\SmartBear2\domains\domain-version_version1.yaml
File saved to: SwaggerHub-Downloads\SmartBear\domains\domain2-version_1.0.0.yaml
File saved to: SwaggerHub-Downloads\SmartBear\apis\Petstore-version_1.0.0.yaml
File saved to: SwaggerHub-Downloads\SmartBear\apis\Petstore-version_1.0.0-2.yaml
File saved to: SwaggerHub-Downloads\SmartBear\apis\Petstore-version_3.0.0.yaml
File saved to: SwaggerHub-Downloads\SmartBear2\apis\Petstore1-version_1.0.0.yaml
File saved to: SwaggerHub-Downloads\SmartBear\templates\Petstore_template-version_1.0.0.yaml
File saved to: SwaggerHub-Downloads\SmartBear\templates\Petstore_template-version_1.0.0-2.yaml
File saved to: SwaggerHub-Downloads\SmartBear\apis\SimpleAPI-version_1.0.0.yaml
File saved to: SwaggerHub-Downloads\SmartBear\apis\SimpleAPI-version_2.0.0.yaml
File saved to: SwaggerHub-Downloads\SmartBear2\templates\SimpleAPIT-version_1.0.0.yaml
Exporting documents completed.
Exported 6 API versions.
Exported 3 domain versions.
Exported 3 template versions.

Exporting integrations...
File saved to: SwaggerHub-Downloads\SmartBear\integrations\Petstore-version_1.0.0.json
File saved to: SwaggerHub-Downloads\SmartBear\integrations\Petstore-version_1.0.0-2.json
File saved to: SwaggerHub-Downloads\SmartBear\integrations\Petstore-version_3.0.0.json
File saved to: SwaggerHub-Downloads\SmartBear2\integrations\Petstore1-version_1.0.0.json
File saved to: SwaggerHub-Downloads\SmartBear\integrations\SimpleAPI-version_1.0.0.json
File saved to: SwaggerHub-Downloads\SmartBear\integrations\SimpleAPI-version_2.0.0.json
Finished exporting integrations.

Exporting projects...
File saved to: SwaggerHub-Downloads\SmartBear\projects\Project1.json
File saved to: SwaggerHub-Downloads\SmartBear2\projects\SmartBear2Proj.json
Finished exporting projects.
```
Import run:
```
Testing connection to the host 10.200.30.32.nip.io ...
Connection to the host successful.
Importing documents...
Successfully created document: Petstore version 1.0.0-2 in SmartBear organization.
Successfully created document: Petstore version 1.0.0 in SmartBear organization.
Successfully created document: Petstore version 3.0.0 in SmartBear organization.
Successfully created document: SimpleAPI version 1.0.0 in SmartBear organization.
Successfully created document: SimpleAPI version 2.0.0 in SmartBear organization.
Successfully created document: domain version 1.0.0 in SmartBear organization.
Successfully created document: domain2 version 1.0.0 in SmartBear organization.
Successfully created document: Petstore_template version 1.0.0-2 in SmartBear organization.
Successfully created document: Petstore_template version 1.0.0 in SmartBear organization.
Successfully created document: Petstore1 version 1.0.0 in SmartBear2 organization.
Successfully created document: domain version version1 in SmartBear2 organization.
Successfully created document: SimpleAPIT version 1.0.0 in SmartBear2 organization.
Finished importing documents.

Importing integrations...
Successfully created integration API Auto Mocking in API Petstore version 1.0.0-2 in SmartBear organization.
Successfully created integration API Auto Mocking in API Petstore version 1.0.0 in SmartBear organization.
Successfully created integration API Auto Mocking in API Petstore version 3.0.0 in SmartBear organization.
Successfully created integration API Auto Mocking in API SimpleAPI version 1.0.0 in SmartBear organization.
Failed to create integration Webhook2 in API SimpleAPI version 1.0.0 in SmartBear organization: {"code":400,"message":"Required 'url' field is not set"}
Successfully created integration API Auto Mocking in API SimpleAPI version 2.0.0 in SmartBear organization.
Successfully created integration API Auto Mocking in API Petstore1 version 1.0.0 in SmartBear2 organization.
Failed to create integration Webhook in API Petstore1 version 1.0.0 in SmartBear2 organization: {"code":400,"message":"Required 'url' field is not set"}
Finished importing integrations.

Importing projects...
Successfully created project Project1 in organization SmartBear
Successfully created project SmartBear2Proj in organization SmartBear2
Finished importing projects.
```
