package com.mdl.platform.support;

/**
 * Utility for conditional integration tests that require Docker.
 */
public final class DockerTestSupport {

    private DockerTestSupport() {
    }

    public static boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor() == 0;
        } catch (Exception ex) {
            return false;
        }
    }
}
