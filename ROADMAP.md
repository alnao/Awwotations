Italian document of Roadmap, TodoList and Done List. 

# TODO LIST


- script senza cloudFront e senza sito!

- lambda per creare user da CLI (without API gateway)
    - script per registrare user via CLI (without API gateway)
    - script per creare un JWT della durata di 1 mese via CLI (without API gateway)
- campi board : color, icon, position(x,y), 
- tabella di log di una note, 
- creare frontend-jx per applicazione desktop!

# DONE
- How it cost on readme
- Test deployment and destroy with specific scripts 
- Created user "alnao@alnao.it A.solita"
    - on Dynamo get user's UUID and used into JWT generator on sub item, on JWT confirated on application
- ciao, on Board i wanna add field "order" (integer >0) e flag favorites (yes/no). change table, api, doc, openapi.
- add "IP table list" enabled, change lambda auth & create script to manage IP on DynamoDB


# AI proposals
- Delete duplicate user on user table
- Clean old code in backend lambda
- Add option to link notes (always visible, no lifecycle)
- Add board  description
- Add board tags
- Add note type as code-type with label
