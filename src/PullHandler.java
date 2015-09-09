/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 ChalkPE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2015-09-09
 */
public class PullHandler {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    public static void main(String[] args) throws IOException, InterruptedException {
        if(args.length < 1){
            System.err.println("java PullHandler <git-repo>");
            return;
        }

        final Path path = Paths.get(args[0]);
        System.out.println("Working on: " + path.toAbsolutePath());

        if(Files.notExists(path) || !Files.isDirectory(path)){
            throw new FileNotFoundException();
        }

        try(final WatchService watcher = FileSystems.getDefault().newWatchService()){
            path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            while(true){
                final WatchKey key = watcher.take();

                for(WatchEvent<?> watchEvent : key.pollEvents()){
                    final WatchEvent.Kind<?> kind = watchEvent.kind();

                    if(kind == StandardWatchEventKinds.OVERFLOW) continue;

                    final WatchEvent<Path> pathWatcher = (WatchEvent<Path>) watchEvent;
                    final Path filename = pathWatcher.context();

                    final Path pullPlease = path.resolve(filename);

                    if(filename.toString().equals("PULL_PLEASE")){
                        final Path pullResult = path.resolve("PULL_RESULT");

                        Files.move(pullPlease, pullResult, StandardCopyOption.REPLACE_EXISTING);
                        Files.copy(new ProcessBuilder("git", "pull").directory(path.toFile()).start().getInputStream(), pullResult, StandardCopyOption.REPLACE_EXISTING);

                        System.out.println("Pulled: " + DATE_FORMAT.format(new Date()));
                    }
                }

                if(!key.reset()) break;
            }
        }
    }
}
