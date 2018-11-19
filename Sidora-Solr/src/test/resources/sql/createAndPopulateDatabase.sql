-- FOR TESTING USING DERBY (NOTE: SOME DATA TYPES ARE DIFFERENT THEN FROM MYSQL)
CREATE SCHEMA fedora3;
CREATE SCHEMA sidora;

SET schema sidora;
CREATE TABLE sidora.camelSolrReindexing ("id" INT not null primary key GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "pid" VARCHAR(255), "origin" VARCHAR(255), "dsLabel" VARCHAR(255), "state" VARCHAR(255), "methodName" VARCHAR(255), "solrOperation" VARCHAR(255), "solrIndex" VARCHAR(255), "foxml" CLOB, "solrDoc" CLOB, "solrStatus" VARCHAR(255), "startTime" TIMESTAMP, "endTime" TIMESTAMP, "elapsed" TIME, "created" TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "updated" TIMESTAMP DEFAULT CURRENT_TIMESTAMP);

SET schema fedora3;
CREATE TABLE fedora3.doRegistry ("doPID" varchar(64), "systemVersion" int NOT NULL DEFAULT 0, "ownerId" varchar(64) DEFAULT NULL, "objectState" varchar(1) DEFAULT 'A', "label" varchar(255) DEFAULT '', PRIMARY KEY ("doPID"));

-- INSERT INTO sidora.camelBatchRequests values('b0d7500a-34be-467a-8cbd-599c6c37b522', 'si:123456', 'resourceFileList', 'ds_metadata', 'ds_sidora', 'association', 'batchTestUser', null, 3, 3, true, true, '2016-12-20 14:37:08.063', '2016-12-20 14:37:08.063');
--
-- INSERT INTO sidora.camelBatchResources values('b0d7500a-34be-467a-8cbd-599c6c37b522', 'image1.jpg', 'si:123456', 'test:000001', 'si:generalImageCModel', 'batchTestUser', 'image(1)', true, true, true, true, true, true, true, false, true, true, true, '2016-12-20 14:37:08.063', '2016-12-20 14:37:08.063');
--
-- INSERT INTO sidora.camelBatchResources values('b0d7500a-34be-467a-8cbd-599c6c37b522', 'image2.jpg', 'si:123456', 'test:000002', 'si:generalImageCModel', 'batchTestUser', 'image(2)', true, true, true, true, true, true, true, false, true, true, true, '2016-12-20 14:37:08.063', '2016-12-20 14:37:08.063');
--
-- INSERT INTO sidora.camelBatchResources values('b0d7500a-34be-467a-8cbd-599c6c37b522', 'image3.jpg', 'si:123456', 'test:000003', 'si:generalImageCModel', 'batchTestUser', 'image(3)', true, true, true, true, true, true, true, false, true, true, true, '2016-12-20 14:37:08.063', '2016-12-20 14:37:08.063');