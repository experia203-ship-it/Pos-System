
package com.connectors.pos.security;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.JwtException;


import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtService {
//for creating a jwt key
@Value("${jwt.secret}")
private String secretKey;

private static final long expTime = 86400000;
private SecretKey hashedKey;

private JwtParser jwtParser;


@PostConstruct
protected void init(){

hashedKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

jwtParser= Jwts.parser().verifyWith(hashedKey).build();
}


public String generateToken(UserDetails userDetails){

    return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis()+expTime))
            .signWith(hashedKey)
            .compact();
}

public String getUsernameFromToken(String token){

    return jwtParser.parseSignedClaims(token)
            .getPayload().getSubject();

}


public boolean validateToken(String token , UserDetails details){


    try{

      var claims= jwtParser.parseSignedClaims(token).getPayload();
String username = claims.getSubject();
   boolean isNameCorrect = username.equals(details.getUsername());


   boolean isNotExpired = !claims.getExpiration().before(new Date());

  return isNameCorrect&&isNotExpired;
    }

    catch(JwtException| IllegalArgumentException ex){

      return false;
    }
}

}
