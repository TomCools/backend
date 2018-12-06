-- all datasets from https://github.com/gbif/checklistbank/blob/master/checklistbank-nub/nub-sources.tsv
-- excluding CoL, the GBIF patches and entire organisation or installations which we add below as lists of datasets
-- nom codes: 0=BACTERIAL, 1=BOTANICAL, 2=CULTIVARS, 3=VIRUS, 4=ZOOLOGICAL

INSERT INTO dataset (gbif_key, created_by, modified_by, origin, import_frequency, code, data_access, title) VALUES
    ('00e791be-36ae-40ee-8165-0b2cb0b8c84f', 12, 12, 0, 7, null, 'https://github.com/mdoering/famous-organism/archive/master.zip', 'Species named after famous people'),
    ('046bbc50-cae2-47ff-aa43-729fbf53f7c5', 12, 12, 0, 7, 1, 'http://rs.gbif.org/datasets/protected/ipni.zip', 'International Plant Names Index'),
    ('0938172b-2086-439c-a1dd-c21cb0109ed5', 12, 12, 0, 7, null, 'http://www.irmng.org/export/IRMNG_genera_DwCA.zip', 'The Interim Register of Marine and Nonmarine Genera'),
    ('0e61f8fe-7d25-4f81-ada7-d970bbb2c6d6', 12, 12, 0, 7, null, 'http://ipt.gbif.fr/archive.do?r=taxref-test', 'TAXREF'),
    ('1c1f2cfc-8370-414f-9202-9f00ccf51413', 12, 12, 0, 7, 1, 'http://rs.gbif.org/datasets/protected/euro_med.zip', 'Euro+Med PlantBase data sample'),
    ('1ec61203-14fa-4fbd-8ee5-a4a80257b45a', 12, 12, 0, 7, null, 'http://ipt.taibif.tw/archive.do?r=taibnet_com_all', 'The National Checklist of Taiwan'),
    ('2d59e5db-57ad-41ff-97d6-11f5fb264527', 12, 12, 0, 7, null, 'http://www.marinespecies.org/dwca/WoRMS_DwC-A.zip', 'World Register of Marine Species'),
    ('3f8a1297-3259-4700-91fc-acc4170b27ce', 12, 12, 0, 7, 1, 'http://data.canadensys.net/ipt/archive.do?r=vascan', 'Database of Vascular Plants of Canada (VASCAN)'),
    ('47f16512-bf31-410f-b272-d151c996b2f6', 12, 12, 0, 7, 4, 'http://rs.gbif.org/datasets/clements.zip', 'The Clements Checklist'),
    ('4dd32523-a3a3-43b7-84df-4cda02f15cf7', 12, 12, 0, 7, null, 'http://api.biodiversitydata.nl/v2/taxon/dwca/getDataSet/nsr', 'Checklist Dutch Species Register - Nederlands Soortenregister'),
    ('52a423d2-0486-4e77-bcee-6350d708d6ff', 12, 12, 0, 7, 0, 'http://rs.gbif.org/datasets/dsmz.zip', 'Prokaryotic Nomenclature Up-to-date'),
    ('5c7bf05c-2890-48e8-9b65-a6060cb75d6d', 12, 12, 0, 7, 4, 'http://ipt.zin.ru:8080/ipt/archive.do?r=zin_megophryidae_bufonidae', 'Catalogue of the type specimens of Bufonidae and Megophryidae (Amphibia: Anura) from research collections of the Zoological Institute,'),
    ('65c9103f-2fbf-414b-9b0b-e47ca96c5df2', 12, 12, 0, 7, 4, 'http://ipt.biodiversity.be/archive.do?r=afromoths', 'Afromoths, online database of Afrotropical moth species (Lepidoptera)'),
    ('66dd0960-2d7d-46ee-a491-87b9adcfe7b1', 12, 12, 0, 7, 1, 'http://rs.gbif.org/datasets/grin_archive.zip', 'GRIN Taxonomy'),
    ('672aca30-f1b5-43d3-8a2b-c1606125fa1b', 12, 12, 0, 7, 4, 'http://rs.gbif.org/datasets/msw3.zip', 'Mammal Species of the World'),
    ('6cfd67d6-4f9b-400b-8549-1933ac27936f', 12, 12, 0, 7, null, 'http://api.gbif.org/v1/occurrence/download/request/dwca-type-specimen-checklist.zip', 'GBIF Type Specimen Names'),
    ('7a9bccd4-32fc-420e-a73b-352b92267571', 12, 12, 0, 7, 4, 'http://data.canadensys.net/ipt/archive.do?r=coleoptera-ca-ak', 'Checklist of Beetles (Coleoptera) of Canada and Alaska. Second Edition.'),
    ('7ea21580-4f06-469d-995b-3f713fdcc37c', 12, 12, 0, 7, 1, 'https://github.com/gbif/algae/archive/master.zip', 'GBIF Algae Classification'),
    ('80b4b440-eaca-4860-aadf-d0dfdd3e856e', 12, 12, 0, 30, 4, 'https://github.com/gbif/iczn-lists/archive/master.zip', 'Official Lists and Indexes of Names in Zoology'),
    ('8d431c96-9e2f-4249-8b0a-d875e3273908', 12, 12, 0, 7, 4, 'http://ipt.zin.ru:8080/ipt/archive.do?r=zin_cosmopterigidae', 'Catalogue of the type specimens of Cosmopterigidae (Lepidoptera: Gelechioidea) from research collections of the Zoological Institute, R'),
    ('8dc469b3-8e61-4f6f-b9db-c70dbbc8858c', 12, 12, 0, 7, null, 'https://raw.githubusercontent.com/mdoering/ion-taxonomic-hierarchy/master/classification.tsv', 'ION Taxonomic Hierarchy'),
    ('90d9e8a6-0ce1-472d-b682-3451095dbc5a', 12, 12, 0, 30, 4, 'http://rs.gbif.org/datasets/protected/fauna_europaea.zip', 'Fauna Europaea'),
    ('96dfd141-7bca-4f82-9325-4420d24e0793', 12, 12, 0, 7, 4, 'http://plazi.cs.umb.edu/GgServer/dwca/49CC45D6B497E6D97BDDF3C0D38289E2.zip', 'Spinnengids'),
    ('9ca92552-f23a-41a8-a140-01abaa31c931', 12, 12, 0, 7, null, 'http://rs.gbif.org/datasets/itis.zip', 'Integrated Taxonomic Information System (ITIS)'),
    ('a43ec6d8-7b8a-4868-ad74-56b824c75698', 12, 12, 0, 7, null, 'http://ipt.gbif.pt/ipt/archive.do?r=uac_checklist_madeira', 'A list of the terrestrial fungi, flora and fauna of Madeira and Selvagens archipelagos'),
    ('a6c6cead-b5ce-4a4e-8cf5-1542ba708dec', 12, 12, 0, 7, null, 'https://data.gbif.no/ipt/archive.do?r=artsnavn', 'Artsnavnebasen'),
    ('aacd816d-662c-49d2-ad1a-97e66e2a2908', 12, 12, 0, 7, 1, 'http://ipt.jbrj.gov.br/jbrj/archive.do?r=lista_especies_flora_brasil', 'Brazilian Flora 2020 project - Projeto Flora do Brasil 2020'),
    ('b267ac9b-6516-458e-bea7-7643842187f7', 12, 12, 0, 7, 4, 'http://ipt.zin.ru:8080/ipt/archive.do?r=zin_polycestinae', 'Catalogue of the type specimens of Polycestinae (Coleoptera: Buprestidae) from research collections of the Zoological Institute, Russia'),
    ('bd25fbf7-278f-41d6-bc17-9f08f2632f70', 12, 12, 0, 7, 4, 'http://ipt.biodiversity.be/archive.do?r=mrac_fruitfly_checklist', 'True Fruit Flies (Diptera, Tephritidae) of the Afrotropical Region'),
    ('bf3db7c9-5e5d-4fd0-bd5b-94539eaf9598', 12, 12, 0, 30, 1, 'http://rs.gbif.org/datasets/index_fungorum.zip', 'Index Fungorum'),
    ('c33ce2f2-c3cc-43a5-a380-fe4526d63650', 12, 12, 0, 7, null, 'http://rs.gbif.org/datasets/pbdb.zip', 'The Paleobiology Database'),
    ('c696e5ee-9088-4d11-bdae-ab88daffab78', 12, 12, 0, 7, 4, 'http://rs.gbif.org/datasets/ioc.zip', 'IOC World Bird List, v8.1'),
    ('c8227bb4-4143-443f-8cb2-51f9576aff14', 12, 12, 0, 7, 4, 'http://zoobank.org:8080/ipt/archive.do?r=zoobank', 'ZooBank'),
    ('d8fb1600-d636-4b35-aa0d-d4f292c1b424', 12, 12, 0, 7, 4, 'http://rs.gbif.org/datasets/protected/fauna_europaea-lepidoptera.zip', 'Fauna Europaea - Lepidoptera'),
    ('d9a4eedb-e985-4456-ad46-3df8472e00e8', 12, 12, 0, 7, 1, 'https://zenodo.org/record/1194673/files/dwca.zip', 'The Plant List with literature'),
    ('da38f103-4410-43d1-b716-ea6b1b92bbac', 12, 12, 0, 7, 4, 'http://ipt.saiab.ac.za/archive.do?r=catalogueofafrotropicalbees', 'Catalogue of Afrotropical Bees'),
    ('de8934f4-a136-481c-a87a-b0b202b80a31', 12, 12, 0, 7, null, 'http://www.gbif.se/ipt/archive.do?r=test', 'Dyntaxa. Svensk taxonomisk databas'),
    ('ded724e7-3fde-49c5-bfa3-03b4045c4c5f', 12, 12, 0, 7, 1, 'http://wp5.e-taxonomy.eu/download/data/dwca/cichorieae.zip', 'International Cichorieae Network (ICN): Cichorieae Portal'),
    ('e01b0cbb-a10a-420c-b5f3-a3b20cc266ad', 12, 12, 0, 7, 3, 'http://rs.gbif.org/datasets/ictv.zip', 'ICTV Master Species List'),
    ('e1c9e885-9d8c-45b5-9f7d-b710ac2b303b', 12, 12, 0, 7, null, 'http://ipt.taibif.tw/archive.do?r=taibnet_endemic', 'Endemic species in Taiwan'),
    ('e402255a-aed1-4701-9b96-14368e1b5d6b', 12, 12, 0, 7, 4, 'http://ctap.inhs.uiuc.edu/dmitriev/DwCArchive.zip', '3i - Typhlocybinae Database'),
    ('e768b669-5f12-42b3-9bc7-ede76e4436fa', 12, 12, 0, 7, 4, 'http://plazi.cs.umb.edu/GgServer/dwca/61134126326DC5BE0901E529D48F9481.zip', 'Carabodes cephalotes'),
    ('f43069fe-38c1-43e3-8293-37583dcf5547', 12, 12, 0, 7, 1, 'https://svampe.databasen.org/dwc/DMS_Fun_taxa.zip', 'Danish Mycological Society - Checklist of Fungi'),
    ('56c83fd9-533b-4b77-a67a-cf521816866e', 12, 12, 0, 7, 4, 'http://ipt.pensoft.net/archive.do?r=tenebrionidae_north_america', 'Catalogue of Tenebrionidae (Coleoptera) of North America');
