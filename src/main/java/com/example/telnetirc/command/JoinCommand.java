package com.example.telnetirc.command;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class JoinCommand extends Command {
    private final String channel;

    public JoinCommand(String[] args) {
        super(args);
        if (args.length != 1) throw new IllegalArgumentException("invalid argument count for /join command");
        this.channel = args[0];
    }

    public String getChannel() {
        return channel;
    }
}
