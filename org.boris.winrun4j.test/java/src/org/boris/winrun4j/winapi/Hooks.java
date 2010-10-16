/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Peter Smith
 *******************************************************************************/
package org.boris.winrun4j.winapi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.boris.winrun4j.Delegate;
import org.boris.winrun4j.Native;
import org.boris.winrun4j.NativeHelper;
import org.boris.winrun4j.winapi.User32.MSG;
import org.boris.winrun4j.winapi.User32.POINT;

public class Hooks
{
    public static final long procCallMsgFilter = Native.getProcAddress(User32.library, "CallMsgFilter");
    public static final long procCallNextHookEx = Native.getProcAddress(User32.library, "CallNextHookEx");
    public static final long procSetWindowsHookEx = Native.getProcAddress(User32.library, "SetWindowsHookExW");
    public static final long procUnhookWindowsHookEx = Native.getProcAddress(User32.library, "UnhookWindowsHookEx");
    private static final boolean is64 = NativeHelper.IS_64;

    public static final int WH_MIN = (-1);
    public static final int WH_MSGFILTER = (-1);
    public static final int WH_JOURNALRECORD = 0;
    public static final int WH_JOURNALPLAYBACK = 1;
    public static final int WH_KEYBOARD = 2;
    public static final int WH_GETMESSAGE = 3;
    public static final int WH_CALLWNDPROC = 4;
    public static final int WH_CBT = 5;
    public static final int WH_SYSMSGFILTER = 6;
    public static final int WH_MOUSE = 7;
    public static final int WH_HARDWARE = 8;
    public static final int WH_DEBUG = 9;
    public static final int WH_SHELL = 10;
    public static final int WH_FOREGROUNDIDLE = 11;
    public static final int WH_CALLWNDPROCRET = 12;
    public static final int WH_KEYBOARD_LL = 13;
    public static final int WH_MOUSE_LL = 14;

    public static boolean callMsgFilter(MSG msg, int code) {
        long ptr = 0;
        if (msg != null) {
            ptr = Native.malloc(18);
            ByteBuffer bb = Native.fromPointer(ptr, 18).order(ByteOrder.LITTLE_ENDIAN);
            if (is64) {
                bb.putLong(msg.hWnd);
                bb.putInt(msg.message);
                bb.putInt(0); // alignment
                bb.putLong(msg.wParam);
                bb.putLong(msg.lParam);
                bb.putInt(msg.time);
            } else {
                bb.putInt((int) msg.hWnd);
                bb.putInt(msg.message);
                bb.putInt((int) msg.wParam);
                bb.putInt((int) msg.lParam);
                bb.putInt(msg.time);
            }
            if (msg.pt != null) {
                bb.putInt(msg.pt.x);
                bb.putInt(msg.pt.y);
            } else {
                bb.putInt(0);
            }
        }
        boolean res = NativeHelper.call(procCallMsgFilter, ptr, code) != 0;
        if (ptr != 0) {
            Native.free(ptr);
        }
        return res;
    }

    public static long callNextHookEx(long hook, int code, long wParam, long lParam) {
        return NativeHelper.call(procCallNextHookEx, hook, code, wParam, lParam);
    }

    public static long setWindowsHookEx(int idHook, Delegate lpfn, long hMod, long threadId) {
        return NativeHelper.call(procSetWindowsHookEx, idHook, lpfn.getPointer(), hMod, threadId);
    }

    public static boolean unhookWindowsHookEx(long hook) {
        return NativeHelper.call(procUnhookWindowsHookEx, hook) != 0;
    }

    public static class MouseProcCallback extends Delegate
    {
        private MouseProc proc;

        public MouseProcCallback(MouseProc proc) {
            this.proc = proc;
        }

        protected long callback(long stack) {
            ByteBuffer bb = NativeHelper.getBuffer(stack, 12);
            int code = bb.getInt();
            int id = bb.getInt();
            MOUSEHOOKSTRUCT ms = new MOUSEHOOKSTRUCT();
            ms.p = new POINT();
            ms.p.x = bb.getShort();
            ms.p.y = bb.getShort();
            ms.hwnd = bb.getInt();
            ms.hitTestCode = bb.getInt();
            ms.extraInfo = bb.getInt();
            return proc.cbMouseProc(code, id, ms);
        }
    }

    public interface MouseProc
    {
        int cbMouseProc(int code, int id, MOUSEHOOKSTRUCT struc);
    }

    public static class MOUSEHOOKSTRUCT
    {
        public POINT p;
        public int hwnd;
        public int hitTestCode;
        public int extraInfo;
    }
}