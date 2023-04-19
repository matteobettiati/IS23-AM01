package Server.Model;

import com.google.gson.annotations.Expose;

/**
 Represents a message sent by a user in a chat room.
 */
public record ChatMessage(@Expose String sender, @Expose String content) {

    /**
     Returns a string representation of the ChatMessage object in the format "From {sender}:\n{content}".
     @return a string representation of the ChatMessage object
     */
    @Override
    public String toString() {
        return "From " + this.sender + ":\n" + this.content;
    }
}