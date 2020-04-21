package io.peasoup.inv.run;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.peasoup.inv.Home;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoolReportMarkdown {

    final private NetworkValuablePool pool;
    final private PoolReport report;

    public PoolReportMarkdown(NetworkValuablePool pool, PoolReport report) {
        if (pool == null) {
            throw new IllegalArgumentException("Pool is required");
        }

        if (report == null) {
            throw new IllegalArgumentException("Report is required");
        }

        this.pool = pool;
        this.report = report;
    }

    public void printPoolMarkdown() {
        File outputFile = new File(Home.getCurrent(), "report.md");
        outputFile.delete();

        try {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile("poolreportmarkdown.mustache");

            Writer outputWriter = new FileWriter(outputFile, true);

            // Process all scopes inside same file
            for(Map<String, Object> scopes: getAllScopes()) {
                mustache.execute(outputWriter, scopes);
            }

            outputWriter.flush();
            outputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Map<String, Object>> getAllScopes() {
        List<Map<String, Object>> scopes = new ArrayList();

        for(Inv inv : pool.getTotalInvs()) {
            scopes.add(getScopesFor(inv));
        }

        return scopes;
    }

    private Map<String, Object> getScopesFor(Inv inv) {
        Map scopes = new HashMap<>();
        scopes.put("name", inv.getName());
        scopes.put("scm", inv.getContext().getScm());

        List<Map> requireStatements = new ArrayList<>();
        List<Map> broadcastStatements = new ArrayList<>();

        int index = 0;

        for (Statement statement : inv.getTotalStatements()) {
            Map<String, Object> statementScope = new HashMap<>();

            statementScope.put("label", statement.toString());
            statementScope.put("index", ++index);
            statementScope.put("markdown", statement.getMarkdown());

            // Check if its a broadcast statement.
            if (statement instanceof BroadcastStatement)
                broadcastStatements.add(statementScope);

            // Check if its a require statement.
            if (statement instanceof RequireStatement) {
                Map<Object, BroadcastResponse> responses = pool.getAvailableStatements().get(statement.getName());

                // Check if it has a broadcast response
                if (responses == null || !responses.containsKey(statement.getId()))
                    continue;;

                BroadcastResponse response = responses.get(statement.getId());
                statementScope.put("resolvedBy", response.getResolvedBy());

                requireStatements.add(statementScope);
            }
        }

        scopes.put("requires", requireStatements);
        scopes.put("broadcasts", broadcastStatements);
        scopes.put("requires_len", requireStatements.size());
        scopes.put("broadcasts_len", broadcastStatements.size());

        return scopes;
    }
}
