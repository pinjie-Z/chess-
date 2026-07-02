package thereia.java.chess.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public final class UserAccount {

    private final String userId;
    private final String passWord;
    private final String nickName;

    @JsonCreator
    public UserAccount(@JsonProperty("userId") String userId,
                       @JsonProperty("passWord") String passWord,
                       @JsonProperty("nickName") String nickName) {
        this.userId = userId;
        this.passWord = passWord;
        this.nickName = nickName;
    }
}
