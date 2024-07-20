package org.nguyendevs.suddendeath.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LifeCycle.State;
import org.apache.logging.log4j.message.Message;

public class WrongLocationFixer implements Filter {
    public Result checkMessage(String msg) {
        return msg.contains("Wrong location! (") && msg.contains(") should be (") ? Result.DENY : Result.NEUTRAL;
    }

    public State getState() {
        return State.STARTED;
    }

    public void initialize() {
    }

    public boolean isStarted() {
        return false;
    }

    public boolean isStopped() {
        return false;
    }

    public void start() {
    }

    public void stop() {
    }

    public Result filter(LogEvent e) {
        return this.checkMessage(e.getMessage().getFormattedMessage());
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object... arg4) {
        return this.checkMessage(msg);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object arg4) {
        return this.checkMessage(msg);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, Object msg, Throwable arg4) {
        return this.checkMessage(msg.toString());
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, Message msg, Throwable arg4) {
        return this.checkMessage(msg.getFormattedMessage());
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object arg4, Object arg5) {
        return this.checkMessage(msg);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object arg4, Object arg5, Object arg6) {
        return this.checkMessage(msg);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object arg4, Object arg5, Object arg6, Object arg7) {
        return this.checkMessage(msg);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
        return this.checkMessage(msg);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9) {
        return this.checkMessage(msg);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10) {
        return this.checkMessage(msg);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
        return this.checkMessage(msg);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12) {
        return this.checkMessage(msg);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String msg, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13) {
        return this.checkMessage(msg);
    }

    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }
}
