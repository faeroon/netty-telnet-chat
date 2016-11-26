package com.example.telnetirc.command;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class LoginCommand extends Command {

    private final String name;
    private final String password;


    public LoginCommand(String[] args) {
        super(args);
        if(args.length != 2) throw new IllegalArgumentException("invalid arguments count for /login command");
        this.name = args[0];
        this.password = args[1];
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }
}
