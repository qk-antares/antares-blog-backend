package org.example.antares.search.controller;

import org.example.antares.common.model.response.R;
import org.example.antares.search.manager.SearchFacade;
import org.example.antares.search.model.dto.SearchRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/search")
public class SearchController {
    @Resource
    private SearchFacade searchFacade;

    @PostMapping
    public R searchByType(@RequestBody SearchRequest searchRequest){
        return R.ok(searchFacade.search(searchRequest));
    }
}
