package com.unknown.security.service.shizuku;

interface IUnknownShellService {
    /**
     * Runs "sh -c <command>" inside the Shizuku server process (shell uid).
     * Returns: first line is the exit code, the rest is combined output.
     */
    String exec(String command);
}
