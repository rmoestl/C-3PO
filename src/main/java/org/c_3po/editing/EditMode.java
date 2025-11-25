package org.c_3po.editing;

import org.c_3po.cmd.CmdArguments;
import org.c_3po.generation.GenerationException;
import org.c_3po.generation.SiteGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditMode {
    private static final Logger LOG = LoggerFactory.getLogger(EditMode.class);

    public static Path getFileToEditFrom(CmdArguments cmdArgs) {
        return Paths.get(cmdArgs.getSourceDirectory(), cmdArgs.getFileToEdit());
    }

    // Note: Not sure accepting a `SiteGenerator` object is good or bad. It
    // could turn out to be perfectly valid to construct the SiteGenerator in
    // the method.
    public static void start(Path filePath, SiteGenerator siteGenerator) throws Exception {
        var serveProcess = startServing(siteGenerator.getDestinationDirectoryPath());
        var editProcess = startEditing(filePath);
        var generatorExecutorService = startGenerator(siteGenerator);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Received shutdown signal. About to stop all started OS processes and internal threads.");

            // Note: Below calls are safe to be called repeatedly. In others
            // words even if processes and executors are already shut down,
            // repeating so has no bad effect.
            serveProcess.destroy();
            editProcess.destroy();
            generatorExecutorService.shutdownNow();
        }));

        // Note: Blocks until editProcess ends.
        editProcess.waitFor();

        // End any other supporting processes.
        serveProcess.destroy();

        // Note: `shutdownNow()` sends an interrupt to the thread(s)
        // the executor service controls. This means, the code run by
        // the threads need to support interruption. `generateOnFileChange`
        // is doing so.
        //
        // Why do we use thread interruption and not some sort of (naive)
        // cancellation flag? Because Brian Goetz outlines the
        // problems of the latter in Java Concurrency in Practice, short JCiP,
        // and instead advices to use thread interruption:
        // "Interruption is usually the most sensible way to implement
        // cancellation."
        //
        // What about `shutdown`. Yep, would be an alternative. It does not
        // use thread interruption. Instead, it allows already running tasks
        // to complete, does not accept any new ones nor starts any already
        // accepted but not yet started ones. However, `generateOnFileChange`
        // is not quite ready for this. It would be if the underlying calls
        // like `buildPagesAndAssets` would be wrapped in tasks submitted to
        // an executor service. This would make up for a nice way of graceful
        // shutdown, i.e. by completing work already begun and then quit.
        // However, we've yet to find the time to tackle this big refactoring.
        generatorExecutorService.shutdownNow();
    }

    private static Process startServing(Path destinationDirectoryPath) throws IOException {

        // Note: Depends on python3 being installed. May could be replaced
        // with a JVM-internal webserver like the "JEP 408: Simple Web Server".
        // But for now, this will do.
        var pb = new ProcessBuilder("python3", "-m", "http.server", "--directory",
                destinationDirectoryPath.toAbsolutePath().toString());
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        return pb.start();
    }

    private static Process startEditing(Path filePath) throws IOException {

        // --wait is important because then gnome-terminal waits for the child process (i.e. vim)
        // to exit. If not set, the gnome-terminal process would exit right after starting vim and
        // this would be bad because our program wants to know when vim is exiting to shut down the
        // other processes as well.
        // Note: Using gnome-terminal is very OS-dependant, yes. We could improve by supporting
        // multiple systems, or we could at least abort operation when gnome-terminal is missing.
        // However, there was no time for that and YAGNI.
        var pb = new ProcessBuilder("gnome-terminal", "--wait", "--", "vim",
                filePath.toAbsolutePath().toString());
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        return pb.start();
    }

    private static ExecutorService startGenerator(SiteGenerator siteGenerator) {
        var executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() -> {
            try {
                siteGenerator.generateOnFileChange();
            } catch (IOException | GenerationException e) {
                LOG.error("Caught an exception while generating the site in edit mode. Rethrowing.");
                throw new RuntimeException(e);
            }
        });

        return executorService;
    }
}
