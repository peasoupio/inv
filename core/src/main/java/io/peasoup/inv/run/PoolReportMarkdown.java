package io.peasoup.inv.run;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.peasoup.inv.Logger;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class PoolReportMarkdown {

    private final NetworkValuablePool pool;

    public PoolReportMarkdown(NetworkValuablePool pool) {
        if (pool == null) {
            throw new IllegalArgumentException("Pool is required");
        }

        this.pool = pool;
    }

    public void printPoolMarkdown() {

        // Make sure latest run folder exists.
        if (!RunsRoller.getLatest().folder().exists()) {
            Logger.warn("Cannot print markdown without a run folder.");
            return;
        }

        // Make sure latest run report folder exists (pe: ".runs/1/reports")
        File reportFolder = new File(RunsRoller.getLatest().folder(), "reports");
        ResourceGroovyMethods.deleteDir(reportFolder);

        // Create new MustacheFactory
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("poolreportmarkdown.mustache");

        // Write new report to file
        for(Map<String, Object> scopes: getAllScopes()) {
            String reportFilename = scopes.get("name").toString() + ".md";
            File outputFile = new File(reportFolder, reportFilename);

            // Use the output file parent in case name as slashes.
            if (!outputFile.getParentFile().exists()) {
                boolean reportFolderCreated = outputFile.getParentFile().mkdirs();
                if (!reportFolderCreated) {
                    Logger.error(new RuntimeException("Report folder could not be created at :" + outputFile.getParentFile().getAbsolutePath()));
                    return;
                }
            }

            // Write report file for INV scope.
            try (Writer outputWriter = new FileWriter(outputFile, false)) {
                mustache.execute(outputWriter, scopes);
                outputWriter.flush();
            } catch (IOException e) {
                Logger.error(e);
            }
        }
    }

    private List<Map<String, Object>> getAllScopes() {
        List<Map<String, Object>> scopes = new ArrayList<>();

        ArrayList<Inv> sorted = new ArrayList<>(pool.getTotalInvs());
        sorted.sort(Comparator.comparing(Inv::getName));

        for(Inv inv : sorted) {
            scopes.add(getScopesFor(inv));
        }

        return scopes;
    }

    private Map<String, Object> getScopesFor(Inv inv) {
        Map<String, Object> scopes = new HashMap<>();
        scopes.put("name", inv.getName());
        scopes.put("markdown", inv.getMarkdown());
        scopes.put("repo", inv.getContext().getRepo());

        List<Map<String, Object>> statements = new ArrayList<>();

        int index = 0;

        for (Statement statement : inv.getTotalStatements()) {
            Map<String, Object> statementScope = new HashMap<>();

            statementScope.put("label", statement.getLabel());
            statementScope.put("index", ++index);
            statementScope.put("markdown", statement.getMarkdown());

            // Check if its a require statement.
            if (statement instanceof RequireStatement) {
                Map<Object, BroadcastResponse> responses = pool.getAvailableStatements().get(statement.getName());

                // Check if it has a broadcast response
                if (responses != null && responses.containsKey(statement.getId())) {
                    BroadcastResponse response = responses.get(statement.getId());
                    statementScope.put("resolvedBy", response.getResolvedBy());
                }
            }

            statements.add(statementScope);
        }

        scopes.put("statements", statements);
        scopes.put("statements_len", statements.size());

        return scopes;
    }
}
