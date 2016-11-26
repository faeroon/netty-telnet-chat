package com.example.telnetirc.command;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class ChatCommand extends Command {

    private final String message;

    public ChatCommand(String[] args) {
        super(args);
        if (args.length != 1) throw new IllegalArgumentException("chat message can not be empty");
        this.message = args[0];
    }

    public String getMessage() {
        return message;
    }
}
