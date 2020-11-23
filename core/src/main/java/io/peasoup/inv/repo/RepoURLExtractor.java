package io.peasoup.inv.repo;

import io.peasoup.inv.Home;
import io.peasoup.inv.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class RepoURLExtractor {

    private RepoURLExtractor() {
        // private ctor
    }

    public static File extract(String repoUrl) {
        if (!UrlValidator.getInstance().isValid(repoUrl))
            return null;

        try {
            HttpURLConnection repoConn = (HttpURLConnection)new URL(repoUrl).openConnection();
            if (HttpURLConnection.HTTP_OK != repoConn.getResponseCode())
                return null;

            File localRepofile = new File(Home.getCurrent(), FilenameUtils.getName(repoUrl));
            if (localRepofile.exists() && !localRepofile.delete())
                return null;

            BufferedReader bf = new BufferedReader(new InputStreamReader(repoConn.getInputStream()));
            FileUtils.writeLines(localRepofile, bf.lines().collect(Collectors.toList()));

            return localRepofile;
        } catch (IOException e) {
            Logger.error(e);
            return null;
        }
    }

}
