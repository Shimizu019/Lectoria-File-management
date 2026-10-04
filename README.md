# Lectoria

An offline Android app that keeps lecture files organised as **Class → Subject → File**.

Students get files from Messenger, Gmail or Drive. Instead of downloading each file and
moving it around the phone's file manager, they pick the class and subject once and Lectoria
copies the file into the right folder.

## How files are stored

Everything is copied into the app's **private** storage, so no storage permission is needed:

```
files/classes/BSIT 3-6/Web Systems/Lecture 01.pdf
```

The filesystem is the source of truth. Screens list folders by reading the disk, and every
create / rename / delete is a real folder operation followed by a refresh.

Files are imported with the **Storage Access Framework** (`OpenDocument`), which is why the
user never has to grant access to the whole device.

## First flow that works

1. Open Lectoria
2. Create class `BSIT 3-6`
3. Open it and create subject `Web Systems`
4. Tap **Add file** and pick a PDF / PPT / DOCX
5. The file is copied into `Web Systems` and appears in the list

## Project structure

```
app/src/main/java/com/lectoria/
├── MainActivity.kt
├── navigation/LectoriaNavGraph.kt      # Classes -> Subjects -> Files
├── data/
│   ├── model/                           # ClassFolder, SubjectFolder, LectureFile
│   ├── storage/FileStorageManager.kt    # all folder + file operations
│   └── repository/LectoriaRepository.kt
├── ui/
│   ├── classes/                         # ClassesScreen + ClassesViewModel
│   ├── subjects/                        # SubjectsScreen + SubjectsViewModel
│   ├── files/                           # FilesScreen + FilesViewModel
│   └── components/                      # FolderItem, FileItem, EmptyState, dialogs, theme
└── util/                                # FileUtils, Constants
```

Each screen has a ViewModel that exposes a single `StateFlow` of screen state and calls the
repository. The repository is the only thing that talks to storage.

## Build

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK
```

Requirements: JDK 17+, Android SDK with API 35 installed. Set your SDK location in
`local.properties` (`sdk.dir=...`).

## Not built yet (by design)

No AI, quizzes, document chat, OCR, cloud sync, accounts, database, notifications or
file viewers. This stage is only the file-management foundation.