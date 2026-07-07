package com.connectors.pos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.logging.Logger;

@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService ;
    private final CustomUserDetailsService detailsService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {



        //String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;

        //if(authHeader!=null&&authHeader.startsWith("Bearer ")){

          //  token = authHeader.substring(7);
            //email=jwtService.getUsernameFromToken(token);
        //}

        Cookie[] cookies =request.getCookies();

        if(cookies!=null){
            for(Cookie cookie:cookies){

                if("jwt".equals(cookie.getName())){

                    token = cookie.getValue();
                    try{

                        email = jwtService.getUsernameFromToken(token);
                    }
                    catch(Exception e){

                        logger.error("Could not extract username from JWt Cookie ",e);
                    }
                      break;
                }

            }




        }

        if(email!=null && SecurityContextHolder.getContext().getAuthentication()==null){

   UserDetails userDetails = detailsService.loadUserByUsername(email);

if(jwtService.validateToken(token,userDetails)){

    UsernamePasswordAuthenticationToken newToken = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());

newToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

SecurityContextHolder.getContext().setAuthentication(newToken);


}

        }
        filterChain.doFilter(request,response);

    }

    @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        String path=request.getRequestURI();
        return path.startsWith("/auth/");
    }

}
