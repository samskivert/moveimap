//
// $Id$

package com.samskivert.moveimap;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;

/**
 * Migrates messages from one IMAP folder to another, potentially on different servers.
 */
public class MoveIMAP
{
    public static void main (String[] args)
        throws Exception
    {
        if (args.length < 8) {
            System.err.println("Usage: MoveIMAP srcurl username password srcfolder " +
                               "desturl username password destfolder");
            System.err.println("Server URLs are of the form: imap://hostname/ or imaps://hostname/");
            System.exit(255);
        }

        Properties props = new Properties();
        Session session = Session.getInstance(new Properties());
        int idx = 0;

        // open up a connection to our source server
        Store srcstore = createAndConnect(session, args[idx++], args[idx++], args[idx++]);
        Folder source = srcstore.getFolder(new URLName(args[idx++]));

        // and one to our destination server
        Store dststore = createAndConnect(session, args[idx++], args[idx++], args[idx++]);
        Folder dest = dststore.getFolder(new URLName(args[idx++]));

        // first migrate folders with a batch size of 10 to be efficient
        int moved = migrateFolder(source, dest, 10);

        // then go through and do the remaining one at a time so that all except the breaky ones
        // are migrated
        moved += migrateFolder(source, dest, 1);

        System.out.println("Moved " + moved + " messages.");
    }

    protected static Store createAndConnect (Session session, String serverURL,
                                             String username, String password)
        throws Exception
    {
        Store store = session.getStore(new URLName(serverURL));
        System.out.println("Connecting to IMAP server on '" + serverURL + "'...");
        store.connect(username, password);
        return store;
    }

    protected static int migrateFolder (Folder source, Folder dest, int batchSize)
        throws Exception
    {
        try {
            source.open(Folder.READ_WRITE);
        } catch (FolderNotFoundException fnfe) {
            System.err.println("Unable to find folder '" + source + "'.");
            System.exit(255);
        }

        try {
            dest.open(Folder.READ_WRITE);
        } catch (FolderNotFoundException fnfe) {
            System.err.println("Unable to find folder '" + dest + "'.");
            System.exit(255);
        }

        int moved = 0;
        List<MimeMessage> msgs = new ArrayList<MimeMessage>();
        List<Integer> msgIds = new ArrayList<Integer>();
        for (Message msg : source.getMessages()) {
            if (msg.getFlags().contains(Flags.Flag.DELETED)) {
                continue; // skip messages marked as deleted
            }
            msgs.add(new MimeMessage((MimeMessage)msg));
            msgIds.add(msg.getMessageNumber());
            if (msgs.size() == batchSize) {
                if (moveMessages(msgs, msgIds, source, dest)) {
                    moved += msgs.size();
                }
                msgs.clear();
                msgIds.clear();
            }
        }
        if (msgs.size() > 0) {
            if (moveMessages(msgs, msgIds, source, dest)) {
                moved += msgs.size();
            }
        }
        if (moved > 0) {
            System.out.println("");
        }

        source.close(false);
        dest.close(false);

        return moved;
    }

    protected static boolean moveMessages (List<MimeMessage> msgs, List<Integer> msgIds,
                                           Folder source, Folder dest)
    {
        try {
            // first add the messages to the destination folder
            dest.appendMessages(msgs.toArray(new MimeMessage[msgs.size()]));

            // and now if that didn't freak out, mark the moved messages as deleted
            int[] ids = new int[msgIds.size()];
            int idx = 0;
            for (int msgId : msgIds) {
                ids[idx++] = msgId;
            }
            source.setFlags(ids, new Flags(Flags.Flag.DELETED), true);

            System.out.print(".");
            System.out.flush();
            return true;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
