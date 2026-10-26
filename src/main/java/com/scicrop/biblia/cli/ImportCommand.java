package com.scicrop.biblia.cli;

import com.scicrop.biblia.service.AlignmentImportService;
import com.scicrop.biblia.service.AlmeidaImportService;
import com.scicrop.biblia.service.BlivreImportService;
import com.scicrop.biblia.service.MaculaImportService;
import com.scicrop.biblia.service.VersificationImportService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

/**
 * Command line entry point for text imports, always with --import.edition=CODE. Does nothing unless
 * one of these is given:
 * --import.blivre.dir=DIR (Biblia Livre f4 directory),
 * --import.macula.file=FILE (MACULA Greek or Hebrew TSV; --import.replace=true reloads a filled edition), or
 * --import.alignment.tokens=FILE with --import.alignment.file=FILE and --import.alignment.original=CODE
 * (Clear Bible Alignments translation tokens and word alignment), or
 * --import.gutenberg.file=FILE (Old Testament of the Almeida 1911, Project Gutenberg eBook #62383), or
 * --import.versification.file=FILE with --import.versification.corrections=FILE and
 * --import.versification.version=COMMIT (TVTMS standard references for every edition; no --import.edition).
 * Run with --spring.main.web-application-type=none (see scripts/import-*.sh).
 */
@Component
public class ImportCommand implements ApplicationRunner {

    private final BlivreImportService blivreImportService;
    private final MaculaImportService maculaImportService;
    private final AlignmentImportService alignmentImportService;
    private final AlmeidaImportService almeidaImportService;
    private final VersificationImportService versificationImportService;

    public ImportCommand(BlivreImportService blivreImportService, MaculaImportService maculaImportService,
                         AlignmentImportService alignmentImportService, AlmeidaImportService almeidaImportService,
                         VersificationImportService versificationImportService) {
        this.blivreImportService = blivreImportService;
        this.maculaImportService = maculaImportService;
        this.alignmentImportService = alignmentImportService;
        this.almeidaImportService = almeidaImportService;
        this.versificationImportService = versificationImportService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String blivreDir = option(args, "import.blivre.dir");
        String maculaFile = option(args, "import.macula.file");
        String alignmentTokens = option(args, "import.alignment.tokens");
        String gutenbergFile = option(args, "import.gutenberg.file");
        String versificationFile = option(args, "import.versification.file");
        if (versificationFile != null) {
            versificationImportService.importAll(Path.of(versificationFile),
                    Path.of(required(args, "import.versification.corrections")),
                    required(args, "import.versification.version"));
            return;
        }
        if (blivreDir == null && maculaFile == null && alignmentTokens == null && gutenbergFile == null) {
            return;
        }
        String editionCode = required(args, "import.edition");
        if (gutenbergFile != null) {
            almeidaImportService.importOldTestament(editionCode, Path.of(gutenbergFile));
        } else if (blivreDir != null) {
            blivreImportService.importEdition(editionCode, Path.of(blivreDir));
        } else if (maculaFile != null) {
            maculaImportService.importEdition(editionCode, Path.of(maculaFile),
                    Boolean.parseBoolean(option(args, "import.replace")));
        } else {
            alignmentImportService.importTranslation(editionCode, Path.of(alignmentTokens),
                    required(args, "import.alignment.original"), Path.of(required(args, "import.alignment.file")));
        }
    }

    private static String option(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    private static String required(ApplicationArguments args, String name) {
        String value = option(args, name);
        if (value == null) {
            throw new IllegalArgumentException("Missing --" + name);
        }
        return value;
    }
}
