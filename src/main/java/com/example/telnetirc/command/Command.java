package com.example.telnetirc.command;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public abstract class Command {

    public Command(String[] args) {
        if(args == null) throw new IllegalArgumentException("args can not be null");
    }
}
