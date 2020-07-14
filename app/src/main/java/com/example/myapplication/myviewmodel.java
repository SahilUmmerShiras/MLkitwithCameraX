package com.example.myapplication;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class myviewmodel extends ViewModel {


    MutableLiveData<String> text;
    public MutableLiveData<String> getText()
    {
        if(text==null)
        {
            text = new MutableLiveData<>();
            text.setValue(" ");
        }

        return text;
    }

    public void changetext(String mtext)
    {
        if(text!=null)
        {
            text.setValue(mtext);
        }

    }
}
