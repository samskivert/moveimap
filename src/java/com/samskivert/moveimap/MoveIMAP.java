//
// $Id$

package com.samskivert.moveimap;

/**
 * Migrates messages from one IMAP folder to another, potentially on different servers.
 */
public class MoveIMAP
{
    public static void main (String[] args)
    {
        if (args.length < 8) {
            System.err.println("Usage: MoveIMAP srchost username password srcfolder " +
                               "desthost username password destfolder");
            System.exit(-1);
        }

        int idx = 0;
        IMAPInfo source = new IMAPInfo();
        source.hostname = args[idx++];
        source.username = args[idx++];
        source.password = args[idx++];
        source.folder = args[idx++];
        IMAPInfo dest = new IMAPInfo();
        dest.hostname = args[idx++];
        dest.username = args[idx++];
        dest.password = args[idx++];
        dest.folder = args[idx++];

        System.out.println("Read source " + source);
        System.out.println("Read dest " + dest);
    }

    protected static class IMAPInfo
    {
        public String hostname;
        public String username;
        public String password;
        public String folder;

        public String toString () {
            return username + "@" + hostname + ":" + folder;
        }
    }
}
